IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF NOT EXISTS(SELECT 1 FROM roles WHERE name='ROLE_ADMIN') INSERT roles(name) VALUES('ROLE_ADMIN');
IF NOT EXISTS(SELECT 1 FROM roles WHERE name='ROLE_EMPLOYEE') INSERT roles(name) VALUES('ROLE_EMPLOYEE');
DECLARE @changes TABLE(id INT,user_id INT,employee_code VARCHAR(20),username NVARCHAR(255),old_position NVARCHAR(100),new_position NVARCHAR(100),old_role VARCHAR(100),new_role VARCHAR(100));
;WITH classified AS (
 SELECT e.id,e.user_id,e.employee_code,u.username,e.position,r.name role_name,
 CASE WHEN u.username COLLATE Latin1_General_100_CI_AI=N'admin' OR e.position COLLATE Latin1_General_100_CI_AI LIKE N'%quan ly%' OR e.position COLLATE Latin1_General_100_CI_AI LIKE N'%quan tri%' OR e.position COLLATE Latin1_General_100_CI_AI LIKE N'%admin%' THEN N'Admin' ELSE N'Nhân viên' END next_position
 FROM employees e LEFT JOIN users u ON u.id=e.user_id LEFT JOIN roles r ON r.id=u.role_id
)
INSERT @changes SELECT id,user_id,employee_code,username,position,next_position,role_name,CASE WHEN next_position=N'Admin' THEN 'ROLE_ADMIN' ELSE 'ROLE_EMPLOYEE' END
FROM classified WHERE COALESCE(position,N'') COLLATE Latin1_General_100_BIN2<>next_position COLLATE Latin1_General_100_BIN2 OR COALESCE(role_name,'')<>CASE WHEN next_position=N'Admin' THEN 'ROLE_ADMIN' ELSE 'ROLE_EMPLOYEE' END;
UPDATE e SET position=c.new_position FROM employees e JOIN @changes c ON c.id=e.id;
UPDATE u SET role_id=r.id FROM users u JOIN @changes c ON c.user_id=u.id JOIN roles r ON r.name=c.new_role;
SELECT id,employee_code,username,old_position,new_position,old_role,new_role FROM @changes ORDER BY id;
COMMIT;
PRINT N'HOÀN TẤT 019_chuc_vu_phan_quyen.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
