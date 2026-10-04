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
@Story("14 Filtering by Status and Categories")
@Severity(SeverityLevel.NORMAL)
public class StatusCategoryFilterTest extends ECommerceBaseTest
{
	@Test
	@Description("FINDING: order status filter not supported")
	public void ordersStatusFilterIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_ORDERS_ENDPOINT, null, null, null,
				query("status", "pending"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id").size(),
				get(ECOMMERCE_ORDERS_ENDPOINT).jsonPath().getList("id").size(),
				"status filter ignored - same length as unfiltered");
	}

	@Test
	public void unknownStatusDoesNotEmptyResults()
	{
		Response response = call("GET", ECOMMERCE_ORDERS_ENDPOINT, null, null, null,
				query("status", "totally-made-up"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.jsonPath().getList("$").size() > 0,
				"unknown status does not empty the result");
	}

	@Test
	@Description("FINDING: products have no category field to filter on")
	public void productsCategoryFilterIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("category", "electronics"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id").size(),
				get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id").size(),
				"category filter ignored");
	}

	@Test
	public void combinedFilterParamsAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("inStock", "true", "status", "active"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id").size(),
				get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id").size(),
				"all filter params ignored together");
	}

	@Test
	public void paymentsStatusFilterReturns404()
	{
		assertNotFound(call("GET", ECOMMERCE_PAYMENTS_ENDPOINT, null, null, null,
				query("status", "completed")));
	}
}
