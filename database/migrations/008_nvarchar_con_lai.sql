-- Chỉ chạy trên database test; triển khai thật phải bật session context rõ ràng.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    -- Giữ mã, enum, username/password băm và actor (username); còn lại dùng Unicode.
    SELECT t.object_id,t.name table_name,c.column_id,c.name column_name,
           c.max_length,c.is_nullable,ty.name type_name,c.collation_name
    INTO #unicode_columns
    FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id
    JOIN sys.types ty ON ty.user_type_id=c.user_type_id
    WHERE t.schema_id=SCHEMA_ID(N'dbo') AND ty.name IN(N'varchar',N'char',N'text')
      AND c.name NOT IN (N'code',N'customer_code',N'employee_code',N'invoice_code',N'order_code',
          N'voucher_code',N'order_type',N'status',N'payment_method',N'payment_status',N'discount_type',
          N'username',N'password',N'actor',N'so_cccd',N'ma_mau')
      AND NOT (t.name=N'roles' AND c.name=N'name')
      -- 011/013: so_cccd và ma_mau là mã ASCII, giữ VARCHAR theo hợp đồng schema.
      -- Migration 009 quy định số điện thoại sổ địa chỉ là VARCHAR(20).
      AND NOT (t.name=N'dia_chi_nhan_hang' AND c.name=N'so_dien_thoai');

    -- Giữ nguyên tính duy nhất, thứ tự khóa, INCLUDE và bộ lọc của index phụ thuộc.
    SELECT DISTINCT i.object_id,i.index_id,t.name table_name,i.name index_name,
        CAST(CASE WHEN i.is_unique_constraint=1 THEN
            N'ALTER TABLE dbo.'+QUOTENAME(t.name)+N' DROP CONSTRAINT '+QUOTENAME(i.name)+N';'
            ELSE N'DROP INDEX '+QUOTENAME(i.name)+N' ON dbo.'+QUOTENAME(t.name)+N';' END AS NVARCHAR(MAX)) drop_sql,
        CAST(CASE WHEN i.is_unique_constraint=1 THEN
            N'ALTER TABLE dbo.'+QUOTENAME(t.name)+N' ADD CONSTRAINT '+QUOTENAME(i.name)+N' UNIQUE '
            ELSE N'CREATE '+CASE WHEN i.is_unique=1 THEN N'UNIQUE ' ELSE N'' END END
            +CASE WHEN i.type=1 THEN N'CLUSTERED ' ELSE N'NONCLUSTERED ' END
            +CASE WHEN i.is_unique_constraint=1 THEN N'' ELSE N'INDEX '+QUOTENAME(i.name)+N' ON dbo.'+QUOTENAME(t.name)+N' ' END
            +N'('+keys.cols+N')'
            +CASE WHEN included.cols IS NULL THEN N'' ELSE N' INCLUDE ('+included.cols+N')' END
            +CASE WHEN i.filter_definition IS NULL THEN N'' ELSE N' WHERE '+i.filter_definition END+N';' AS NVARCHAR(MAX)) create_sql
    INTO #unicode_indexes
    FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
    CROSS APPLY (SELECT STRING_AGG(CAST(QUOTENAME(c.name)+CASE WHEN ic.is_descending_key=1 THEN N' DESC' ELSE N' ASC' END AS NVARCHAR(MAX)),N',') WITHIN GROUP(ORDER BY ic.key_ordinal) cols
        FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
        WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0) keys
    OUTER APPLY (SELECT STRING_AGG(CAST(QUOTENAME(c.name) AS NVARCHAR(MAX)),N',') WITHIN GROUP(ORDER BY ic.index_column_id) cols
        FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
        WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=1) included
    WHERE i.is_primary_key=0 AND i.type IN(1,2)
      AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN #unicode_columns u ON u.object_id=ic.object_id AND u.column_id=ic.column_id
                 WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id);

    DECLARE @statement NVARCHAR(MAX),@table SYSNAME,@column SYSNAME,@length INT,@nullable BIT,@type SYSNAME,@collation SYSNAME;
    DECLARE drop_indexes CURSOR LOCAL FAST_FORWARD FOR SELECT drop_sql FROM #unicode_indexes;
    OPEN drop_indexes; FETCH NEXT FROM drop_indexes INTO @statement;
    WHILE @@FETCH_STATUS=0 BEGIN EXEC sp_executesql @statement; FETCH NEXT FROM drop_indexes INTO @statement; END;
    CLOSE drop_indexes; DEALLOCATE drop_indexes;
    DECLARE columns_to_convert CURSOR LOCAL FAST_FORWARD FOR
        SELECT table_name,column_name,max_length,is_nullable,type_name,collation_name FROM #unicode_columns ORDER BY table_name,column_id;
    OPEN columns_to_convert; FETCH NEXT FROM columns_to_convert INTO @table,@column,@length,@nullable,@type,@collation;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @statement=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' ALTER COLUMN '+QUOTENAME(@column)+N' NVARCHAR('
            +CASE WHEN @length=-1 OR @length>4000 OR @type=N'text' THEN N'MAX' ELSE CONVERT(NVARCHAR(10),@length) END+N')'
            +CASE WHEN @collation IS NULL THEN N'' ELSE N' COLLATE '+@collation END
            +CASE WHEN @nullable=1 THEN N' NULL;' ELSE N' NOT NULL;' END;
        EXEC sp_executesql @statement;
        FETCH NEXT FROM columns_to_convert INTO @table,@column,@length,@nullable,@type,@collation;
    END;
    CLOSE columns_to_convert; DEALLOCATE columns_to_convert;
    DECLARE create_indexes CURSOR LOCAL FAST_FORWARD FOR SELECT create_sql FROM #unicode_indexes;
    OPEN create_indexes; FETCH NEXT FROM create_indexes INTO @statement;
    WHILE @@FETCH_STATUS=0 BEGIN EXEC sp_executesql @statement; FETCH NEXT FROM create_indexes INTO @statement; END;
    CLOSE create_indexes; DEALLOCATE create_indexes;
    SELECT COUNT(*) converted_columns FROM #unicode_columns;
    DROP TABLE #unicode_indexes;
    DROP TABLE #unicode_columns;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
