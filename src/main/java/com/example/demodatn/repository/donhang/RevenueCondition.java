package com.example.demodatn.repository.donhang;

/** Điều kiện dùng chung cho doanh thu và bán chạy; alias đơn hàng luôn là o. */
public final class RevenueCondition {
    public static final String AMOUNT="CASE WHEN COALESCE(o.shipping_policy_version,0)>=1 THEN o.final_amount-COALESCE(o.shipping_fee,0) ELSE o.final_amount END";
    public static final String SQL = "o.payment_status='PAID' AND o.status NOT IN ('CANCELLED','DELIVERY_FAILED')";

    private RevenueCondition() {
    }
}
