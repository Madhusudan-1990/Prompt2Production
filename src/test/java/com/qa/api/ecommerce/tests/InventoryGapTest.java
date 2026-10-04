package com.qa.api.ecommerce.tests;

import java.util.List;
import java.util.Map;

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
@Story("07 Inventory Management")
@Severity(SeverityLevel.NORMAL)
public class InventoryGapTest extends ECommerceBaseTest
{
	@Test
	public void getInventoryReturns404()
	{
		assertNotFound(get(ECOMMERCE_INVENTORY_ENDPOINT));
	}

	@Test
	public void getInventoryItemReturns404()
	{
		assertNotFound(get(ECOMMERCE_INVENTORY_ITEM_ENDPOINT.replace("{id}", "1")));
	}

	@Test
	public void putInventoryItemReturns405()
	{
		Response response = put(ECOMMERCE_INVENTORY_ITEM_ENDPOINT, json("quantity", 5), path("id", 1));
		assertMethodNotAllowed(response);
	}

	@Test
	public void patchInventoryItemReturns405()
	{
		Response response = patch(ECOMMERCE_INVENTORY_ITEM_ENDPOINT, json("adjustment", -1), path("id", 1));
		assertMethodNotAllowed(response);
	}

	@Test
	@Description("Stock lives on the product resource; orders do not adjust it")
	public void productsExposeIntegerStock()
	{
		Response response = get(ECOMMERCE_PRODUCTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> list = response.jsonPath().getList("$");
		for (Map<String, Object> p : list)
		{
			Object stock = p.get("stock");
			Assert.assertTrue(stock instanceof Number, "stock of " + p.get("name") + " is a number");
			Assert.assertTrue(((Number) stock).doubleValue() == Math.floor(((Number) stock).doubleValue()),
					"integer stock for " + p.get("name"));
		}
		Map<String, Object> first = list.get(0);
		Assert.assertTrue(first.containsKey("stock"), "stock exposed on product");
		Assert.assertFalse(first.containsKey("quantity"), "no separate quantity field");
	}
}
