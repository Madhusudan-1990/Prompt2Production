package com.qa.api.ecommerce.tests;

import org.testng.annotations.Test;

import com.qa.api.ecommerce.base.ECommerceBaseTest;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

@Epic("E-Commerce API")
@Story("06 Shopping Cart")
@Severity(SeverityLevel.NORMAL)
public class ShoppingCartGapTest extends ECommerceBaseTest
{
	@Test
	public void getCartReturns404()
	{
		assertNotFound(get(ECOMMERCE_CART_ENDPOINT));
	}

	@Test
	public void getCartItemsReturns404()
	{
		assertNotFound(get(ECOMMERCE_CART_ITEMS_ENDPOINT));
	}

	@Test
	public void postCartItemsReturns405()
	{
		Response response = post(ECOMMERCE_CART_ITEMS_ENDPOINT, json("product_id", 1, "quantity", 2));
		assertMethodNotAllowed(response);
	}

	@Test
	public void putCartItemReturns405()
	{
		Response response = put(ECOMMERCE_CART_ITEM_ENDPOINT, json("quantity", 5), path("itemId", 1));
		assertMethodNotAllowed(response);
	}

	@Test
	public void deleteCartReturns405()
	{
		assertMethodNotAllowed(delete(ECOMMERCE_CART_ENDPOINT, null));
	}
}
