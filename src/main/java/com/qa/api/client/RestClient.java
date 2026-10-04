package com.qa.api.client;

import java.io.File;
import java.io.ObjectInputFilter.Config;
import java.util.Base64;
import java.util.Map;

import com.qa.api.constants.AuthType;
import com.qa.api.exceptions.APIException;
import com.qa.api.manager.ConfigManager;
import static io.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
/*
 * In API Testing we shouldn't go with parallel execution as the api has already faster execution. Also we are using static block as part of loading the properties file.
 */
//Part1
public class RestClient 
{
	
	// 1d Define Response Specs:
	private ResponseSpecification responseSpec200 =  expect().statusCode(200); // Already expect() method coming static import
	private ResponseSpecification responseSpec201 =  expect().statusCode(201);
	private ResponseSpecification responseSpec204 =  expect().statusCode(204);
	private ResponseSpecification responseSpec400 =  expect().statusCode(400);
	private ResponseSpecification responseSpec404 =  expect().statusCode(404);
	private ResponseSpecification responseSpec200or201 =  expect().statusCode(anyOf(equalTo(200),equalTo(201)));
	private ResponseSpecification responseSpec200or404 =  expect().statusCode(anyOf(equalTo(200),equalTo(404)));
	//1a
	private RequestSpecification setupRequest(String baseUrl,AuthType authType, ContentType contentType)
	{
		RequestSpecification request =  RestAssured.given().log().all()
											.baseUri(baseUrl)
											.contentType(contentType)
											.accept(contentType); // not mandatory to have .accept() 
		
		switch (authType)
		{
		case BEARER_TOKEN:
				request.header("Authorization", "Bearer "+ ConfigManager.get("bearertoken"));
				break;
		case BASIC_AUTH:
			request.header("Authorization", "Basic "+ generateBasicAuthToken());
			break;
		case API_KEY:
			request.header("x-api-key", "Basic "+ "api key");
			break;
		case NO_AUTH:
			System.out.println("Auth is not required...");
			break;
			
		default:
			System.out.println("This is auth is not supported !!! Pkease pass the right Auth Type");
			throw new APIException("======Invalid Auth=======");
				
		}
		return request;
	}
	
	private String generateBasicAuthToken()
	{
		String credentials = ConfigManager.get("basicauthusername") + ":" + ConfigManager.get("basicauthpassword");
		//admin:admin --> ""
		return Base64.getEncoder().encodeToString(credentials.getBytes());
	
	}
	
	//1b
	private void applyParams(RequestSpecification request,Map<String, String> queryParams, Map<String, String> pathParams) // Using Map because we need to support multiple query params based on requirement.
	{
		if(queryParams!=null) // Add Query Param if its null
		{
			request.queryParams(queryParams);
		}
		if(pathParams!=null) // Add Path Param if its null
		{
			request.pathParams(pathParams);
		}
	}
	
	//CRUD:
	
	//1c GET
	/**
	 * This method used to call the Get Calls
	 * @param baseUrl
	 * @param endPoint
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public Response get(String baseUrl, String endPoint,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType){
		
		
	     RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
	     applyParams(request, queryParams, pathParams);
	     Response response = request.get(endPoint).then().spec(responseSpec200or404).extract().response(); // This will return 200 if found else 404 and extract the response
	     response.prettyPrint();
	     return response;
	}
	
	//1d POST
	/**
	 * This method is used to call Post Calls
	 * @param <T>
	 * @param baseUrl
	 * @param endPoint
	 * @param body
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public<T> Response post(String baseUrl, String endPoint,T body, // 'T' is the Reserved keyword in java which takes any Type like Pojo,String,Lombok except File object. We have to give 'T' in the generics. 
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType)
	{
		  RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
		  applyParams(request, queryParams, pathParams);
		  Response response = request.body(body).post(endPoint).then().spec(responseSpec200or201).extract().response();
		  response.prettyPrint();
		     return response;
	}
	public Response post(String baseUrl, String endPoint,File file,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType)
	{
		  RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
		  applyParams(request, queryParams, pathParams);
		  Response response = request.body(file).post(endPoint).then().spec(responseSpec200or201).extract().response();
		  response.prettyPrint();
		     return response;
	}
	public Response post(String baseUrl, String endPoint,
			String grantType,
			String clientId,
			String clientSecret,
			ContentType contentType)
	{
		
		Response  response = RestAssured.given().log().all()
		 				.contentType(contentType)
		 				.formParam("grant_type", grantType)
		 				.formParam("client_id", clientId)
		 				.formParam("client_secret", clientSecret)
		 				.when()
		 					.post(baseUrl+endPoint);
		response.prettyPrint();
		return response;
	}
	
	//1e PUT
	/**
	 * This method is used to call Put Calls
	 * @param <T>
	 * @param baseUrl
	 * @param endPoint
	 * @param body
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public<T> Response put(String baseUrl, String endPoint,T body, // 'T' is the Reserved keyword in java which takes any Type like Pojo,String,Lombok except File object. We have to give 'T' in the generics. 
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType)
	{
		  RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
		  applyParams(request, queryParams, pathParams);
		  Response response = request.body(body).put(endPoint).then().spec(responseSpec200).extract().response();
		  response.prettyPrint();
		     return response;
	}
	
	//1f Patch
		/**
		 * This method is used to call Patch Calls
		 * @param <T>
		 * @param baseUrl
		 * @param endPoint
		 * @param body
		 * @param queryParams
		 * @param pathParams
		 * @param authType
		 * @param contentType
		 * @return
		 */
		public<T> Response patch(String baseUrl, String endPoint,T body, // 'T' is the Reserved keyword in java which takes any Type like Pojo,String,Lombok except File object. We have to give 'T' in the generics. 
				Map<String, String> queryParams,
				Map<String, String> pathParams,
				AuthType authType,
				ContentType contentType)
		{
			  RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
			  applyParams(request, queryParams, pathParams);
			  Response response = request.body(body).patch(endPoint).then().spec(responseSpec200).extract().response();
			  response.prettyPrint();
			     return response;
		}
		
		//1g Delete
				/**
				 * This method is used to call Delete Calls
				 * @param <T>
				 * @param baseUrl
				 * @param endPoint
				 * @param body
				 * @param queryParams
				 * @param pathParams
				 * @param authType
				 * @param contentType
				 * @return
				 */
				public<T> Response delete(String baseUrl, String endPoint, // 'T' is the Reserved keyword in java which takes any Type like Pojo,String,Lombok except File object. We have to give 'T' in the generics. 
						Map<String, String> queryParams,
						Map<String, String> pathParams,
						AuthType authType,
						ContentType contentType)
				{
					  RequestSpecification request= setupRequest(baseUrl, authType, contentType); // Calling setupRequest Method
					  applyParams(request, queryParams, pathParams);
					  Response response = request.delete(endPoint).then().spec(responseSpec204).extract().response(); // This will return 204 if found else 404 and extract the response
					  response.prettyPrint();
					     return response;
				}

	//1h Spec-free execution (no fixed ResponseSpecification) - used when the test itself owns the expected
	// status code (negative cases such as 400/405/422/500, CORS preflight, HEAD). Also supports extra headers
	// (Origin, Access-Control-Request-Method/Headers) which setupRequest() does not add.
	/**
	 * @param method HTTP method (GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD)
	 * @param headers extra request headers or null
	 */
	public Response execute(String method, String baseUrl, String endPoint, Object body,
			Map<String, String> headers,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType)
	{
		RequestSpecification request = setupRequest(baseUrl, authType, contentType);
		if (headers != null)
		{
			headers.forEach(request::header);
		}
		applyParams(request, queryParams, pathParams);
		if (body != null)
		{
			request.body(body);
		}
		Response response = request.request(method, endPoint).then().extract().response();
		response.prettyPrint();
		return response;
	}

	public Response get(String baseUrl, String endPoint, AuthType authType, ContentType contentType)
	{
		return execute("GET", baseUrl, endPoint, null, null, null, null, authType, contentType);
	}

	
	
}
