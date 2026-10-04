package com.qa.api.ecommerce.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
@Story("13 Sorting and Ordering")
@Severity(SeverityLevel.NORMAL)
public class SortingGapTest extends ECommerceBaseTest
{
	@Test
	@Description("FINDING: sort param has no effect")
	public void sortParamIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("sort", "price_desc"));
		Assert.assertEquals(response.statusCode(), 200);

		List<Object> baseline = get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id");
		Assert.assertEquals(response.jsonPath().getList("id"), baseline,
				"sort param ignored - identical order to unsorted list");

		List<Double> prices = response.jsonPath().getList("price", Double.class);
		List<Double> sortedDesc = new ArrayList<>(prices);
		sortedDesc.sort(Collections.reverseOrder());
		Assert.assertNotEquals(prices, sortedDesc, "prices not sorted desc - sort param has no effect");
	}

	@Test
	public void orderByAndDirectionAreIgnored()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("order_by", "price", "direction", "desc"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id"),
				get(ECOMMERCE_PRODUCTS_ENDPOINT).jsonPath().getList("id"), "order_by/direction ignored");
	}

	@Test
	public void ordersSortParamIsIgnored()
	{
		Response response = call("GET", ECOMMERCE_ORDERS_ENDPOINT, null, null, null,
				query("sort", "total_amount"));
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getList("id"),
				get(ECOMMERCE_ORDERS_ENDPOINT).jsonPath().getList("id"), "orders sort param ignored");
	}

	@Test
	@Description("No 400 for unknown sort value - sort is never validated")
	public void nonsenseSortValueIsAccepted()
	{
		Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
				query("sort", "nonsense_field_xyz"));
		Assert.assertEquals(response.statusCode(), 200, "no 400 for unknown sort value");
	}
}
