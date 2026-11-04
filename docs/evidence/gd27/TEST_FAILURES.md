| Test | Trước GD27 | Sau GD27 |
|---|---|---|
| `AdminFrameIntegrationTest.eightSecurityCasesAndForgedCodeIgnored` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `PaginationIntegrationTest.allListsReturnTwelveTwelveSixWithStableIds` | FAIL | FAIL — Cannot invoke "org.springframework.data.domain.Page.getNumberOfElements()" because "data" is null |
| `EmployeeAccessIntegrationTest.eightSecurityCasesProtectNewCommands` | FAIL | FAIL — Status expected:<302> but was:<403> |
| `AccountProvisioningIntegrationTest.administratorRoutesAndCsrfCannotBeBypassed` | FAIL | FAIL — Status expected:<302> but was:<403> |
| `DeliveryIntegrationTest.customerInvoiceOwnershipAndEightSecurityScenarios` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `GeocodingServiceTest.wardCenterIsValidatedAndPersistedOnce` | PASS | FAIL — No value present |
| `InvoiceOnlineWorkspaceIntegrationTest.eightSecurityCasesForOnlineAndInvoice` | FAIL | FAIL — Status expected:<302> but was:<403> |
| `OrderRollbackIntegrationTest.posShortageRollsBackOrderVoucherAndFirstStockWrite` | FAIL | FAIL — Flash attribute 'error' |
| `PosModalPricingIntegrationTest.testPosAvailableQuantityCalculationsInSession` | FAIL | FAIL — Model attribute 'variants' |
| `PosQrIntegrationTest.initialPosHasNoCustomerListAndShortSearchIsEmpty` | FAIL | FAIL — Response content |
| `PosQrIntegrationTest.receiptCheckboxAndExactAmountAreEnforcedBeforeCreatingOrder` | FAIL | FAIL — Flash attribute 'error' |
| `PosQrIntegrationTest.invoiceChannelHasOwnColumnAndEightSecurityGroups` | FAIL | FAIL — Response content |
| `ShippingOnlineIntegrationTest.eightSecurityGroupsAndAuthenticatedReadRoutes` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `AdminChatbotIntegrationTest.testRevenueMetricsMatchDashboardAndIndependentSql` | FAIL | FAIL — expected: <0> but was: <1> |
| `ShippingAddressIntegrationTest.eightSecurityScenarios` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `DiscountWorkflowIntegrationTest.voucherIsAlwaysPublicRetainsCounterCannotDeleteAndSwitchHasCsrf` | FAIL | FAIL — Status expected:<302> but was:<403> |
| `CatalogIntegrationTest.guestPosCheckoutWritesOrderWithoutCreatingDuplicateGuestCustomer` | FAIL | FAIL — Redirected URL '/admin/invoices/465' does not match the expected URL pattern '/admin/pos/success/*' |
| `ColorVariantIntegrationTest.eightSecurityCasesAndOldOrderDisplay` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `EditableVariantIntegrationTest.eightSecurityGroupsAndCrossProductOwnership` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `InitialMaterialIntegrationTest.eightSecurityScenarios` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `ProductFeedbackIntegrationTest.eightSecurityGroupsAndAdminCannotDelete` | FAIL | FAIL — Status expected:<302> but was:<403> |
| `SoleIntegrationTest.labelsAndSoleCrudRoutesUseExistingRoleAndCsrfProtection` | FAIL | FAIL — Range for response status value 403 expected:<REDIRECTION> but was:<CLIENT_ERROR> |
| `DashboardIntegrationTest.cashPlusTransferEqualsTotalPeriodRevenue` | FAIL | FAIL — expected: <0> but was: <1> |
| `StatisticsIntegrationTest.independentSqlMatchesEveryUnifiedDashboardNumberAcrossAllFilterModes` | FAIL | FAIL — expected: <0> but was: <1> |
| `StatisticsIntegrationTest.metricsRankingsBoundariesChannelsAndStatusesUseSnapshotMoney` | FAIL | FAIL — expected: <90.50> but was: <0> |
