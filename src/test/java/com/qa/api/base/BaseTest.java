package com.qa.api.base;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import com.qa.api.client.RestClient;
import com.qa.api.manager.ConfigManager;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;

//@Listeners(ChainTestListener.class)
public class BaseTest 
{
	protected RestClient restClient;
	
	//****************************** API Base URL *******************************/
	protected  static String BASE_URL_ECOMMERCE;

	
	//****************************** E-Commerce EndPoints *******************************/
	protected final static String ECOMMERCE_PRODUCTS_ENDPOINT = "/products";
	protected final static String ECOMMERCE_PRODUCT_ENDPOINT = "/products/{id}";
	protected final static String ECOMMERCE_PRODUCT_SELECT_ENDPOINT = "/products/{id}/select";
	protected final static String ECOMMERCE_PRODUCT_CATEGORIES_ENDPOINT = "/products/categories";
	protected final static String ECOMMERCE_PRODUCT_SEARCH_ENDPOINT = "/products/search";
	protected final static String ECOMMERCE_PRODUCT_CATEGORY_ENDPOINT = "/products/category/{category}";
	protected final static String ECOMMERCE_USERS_ENDPOINT = "/users";
	protected final static String ECOMMERCE_USER_ENDPOINT = "/users/{id}";
	protected final static String ECOMMERCE_ORDERS_ENDPOINT = "/orders";
	protected final static String ECOMMERCE_ORDER_ENDPOINT = "/orders/{id}";
	protected final static String ECOMMERCE_PAYMENTS_ENDPOINT = "/payments";
	protected final static String ECOMMERCE_PAYMENT_ENDPOINT = "/payments/{id}";
	protected final static String ECOMMERCE_CART_ENDPOINT = "/cart";
	protected final static String ECOMMERCE_CART_ITEMS_ENDPOINT = "/cart/items";
	protected final static String ECOMMERCE_CART_ITEM_ENDPOINT = "/cart/items/{itemId}";
	protected final static String ECOMMERCE_INVENTORY_ENDPOINT = "/inventory";
	protected final static String ECOMMERCE_INVENTORY_ITEM_ENDPOINT = "/inventory/{id}";
	protected final static String ECOMMERCE_PURCHASES_ENDPOINT = "/purchases";
	protected final static String ECOMMERCE_OPENAPI_ENDPOINT = "/openapi.json";
	protected final static String ECOMMERCE_HEALTH_ENDPOINT = "/internal/health";
	protected final static String ECOMMERCE_ADMIN_STATS_ENDPOINT = "/admin/stats";
	protected final static String ECOMMERCE_DEBUG_DB_ENDPOINT = "/debug/db";
	
	@BeforeTest
	public void initSetup()
	{
		RestAssured.filters(new AllureRestAssured());
		BASE_URL_ECOMMERCE = ConfigManager.get("baseurl.ecommerce").trim();
	}
	
	@BeforeTest
	public void setup() 
	{
			restClient = new RestClient();
	}
	
	@AfterTest
	public void stopMockServer() 
	{
	}
}
