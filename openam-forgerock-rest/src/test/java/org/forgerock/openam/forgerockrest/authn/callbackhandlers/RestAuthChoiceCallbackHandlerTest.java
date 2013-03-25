/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2013 ForgeRock Inc.
*/

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import org.forgerock.openam.forgerockrest.authn.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.ChoiceCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RestAuthChoiceCallbackHandlerTest {

    private RestAuthCallbackHandler<ChoiceCallback> restAuthChoiceCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthChoiceCallbackHandler = new RestAuthChoiceCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthChoiceCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(ChoiceCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);

        given(request.getParameter("choices")).willReturn("63");

        //When
        boolean updated = restAuthChoiceCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, choiceCallback, HttpMethod.POST);

        //Then
        verify(choiceCallback).setSelectedIndex(63);
        assertTrue(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenChoicesIsNull()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);

        given(request.getParameter("choices")).willReturn(null);

        //When
        boolean updated = restAuthChoiceCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, choiceCallback, HttpMethod.POST);

        //Then
        verify(choiceCallback, never()).setSelectedIndexes(Matchers.<int[]>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldFailToUpdateCallbackFromRequestWhenChoicesIsEmptyString()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);

        given(request.getParameter("choices")).willReturn("");

        //When
        boolean updated = restAuthChoiceCallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, choiceCallback, HttpMethod.POST);

        //Then
        verify(choiceCallback, never()).setSelectedIndexes(Matchers.<int[]>anyObject());
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        ChoiceCallback originalChoiceCallback = mock(ChoiceCallback.class);

        //When
        ChoiceCallback choiceCallback = restAuthChoiceCallbackHandler.handle(headers, request, response, jsonPostBody,
                originalChoiceCallback);

        //Then
        assertEquals(originalChoiceCallback, choiceCallback);
    }

    @Test
    public void shouldConvertToJson() throws JSONException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);

        //When
        JSONObject jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.getString("type"));

        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(3, jsonObject.getJSONArray("output").length());
        assertEquals("Select choice:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals("1", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(0));
        assertEquals("34", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(1));
        assertEquals("66", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(2));
        assertEquals("93", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(3));
        assertEquals(0, jsonObject.getJSONArray("output").getJSONObject(2).getInt("value"));

        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(1, jsonObject.getJSONArray("input").length());
        assertEquals(0, jsonObject.getJSONArray("input").getJSONObject(0).getInt("value"));
    }

    @Test
    public void shouldConvertToJsonWithPreSelectedIndexes() throws JSONException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);
        choiceCallback.setSelectedIndex(1);

        //When
        JSONObject jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.getString("type"));

        assertNotNull(jsonObject.getJSONArray("output"));
        assertEquals(3, jsonObject.getJSONArray("output").length());
        assertEquals("Select choice:", jsonObject.getJSONArray("output").getJSONObject(0).getString("value"));
        assertEquals("1", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(0));
        assertEquals("34", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(1));
        assertEquals("66", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(2));
        assertEquals("93", jsonObject.getJSONArray("output").getJSONObject(1).getJSONArray("value").get(3));
        assertEquals(0, jsonObject.getJSONArray("output").getJSONObject(2).getInt("value"));

        assertNotNull(jsonObject.getJSONArray("input"));
        assertEquals(1, jsonObject.getJSONArray("input").length());
        assertEquals(1, jsonObject.getJSONArray("input").getJSONObject(0).getInt("value"));
    }

    @Test
    public void shouldConvertFromJson() throws JSONException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 1)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select choice:"))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("1").put("34").put("66").put("93")))
                        .put(new JSONObject()
                                .put("value", "0")))
                .put("type", "ChoiceCallback");

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws JSONException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 1)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select choice:"))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("1").put("34").put("66").put("93")))
                        .put(new JSONObject()
                                .put("value", "0")))
                .put("type", "PasswordCallback");

        //When
        restAuthChoiceCallbackHandler.convertFromJson(choiceCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws JSONException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JSONObject jsonNameCallback = new JSONObject()
                .put("input", new JSONArray()
                        .put(new JSONObject()
                                .put("value", 1)))
                .put("output", new JSONArray()
                        .put(new JSONObject()
                                .put("value", "Select choice:"))
                        .put(new JSONObject()
                                .put("value", new JSONArray().put("1").put("34").put("66").put("93")))
                        .put(new JSONObject()
                                .put("value", "0")))
                .put("type", "choicecallback");

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }
}