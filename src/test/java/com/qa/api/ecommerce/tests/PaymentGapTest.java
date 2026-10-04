package com.qa.api.ecommerce.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.ecommerce.base.ECommerceBaseTest;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

@Epic("E-Commerce API")
@Story("05 Payment Integration")
@Severity(SeverityLevel.NORMAL)
public class PaymentGapTest extends ECommerceBaseTest
{
	@Test
	public void getPaymentsReturns404()
	{
		assertNotFound(get(ECOMMERCE_PAYMENTS_ENDPOINT));
	}

	@Test
	public void postPaymentsReturns405()
	{
		Response response = post(ECOMMERCE_PAYMENTS_ENDPOINT,
				json("order_id", 1, "method", "credit_card", "amount", 1));
		assertMethodNotAllowed(response);
	}

	@Test
	public void getPaymentByIdReturns404()
	{
		assertNotFound(get(ECOMMERCE_PAYMENT_ENDPOINT.replace("{id}", "1")));
	}

	@Test
	public void patchPaymentByIdReturns405()
	{
		Response response = patch(ECOMMERCE_PAYMENT_ENDPOINT, json("status", "completed"), path("id", 1));
		assertMethodNotAllowed(response);
	}
}
