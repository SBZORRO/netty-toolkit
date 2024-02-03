package com.sbzorro;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GsonUtil {

  public static final Gson GSON = new Gson();

  public static String okay(int code, JsonObject... j) {
    JsonArray ja = new JsonArray();
    for (JsonObject jj : j) {
      ja.add(jj);
    }
    return okay(code, ja);
  }

  public static String okay(int code, JsonArray ja) {
    JsonObject json = new JsonObject();
    json.addProperty("code", code);
    json.add("data", ja);
    return json.toString();
  }

  public static String okay(String data) {
    int code = data == null ? 500 : 200;
    return okay(code, jsonResp(code, data));
  }

  public static String okay(List<String> li) {
    int code = 200;
    JsonArray ja = new JsonArray();
    for (String s : li) {
      code = s == null && code == 200 ? 500 : 200;
      ja.add(jsonResp(s));
    }
    return okay(code, ja);
  }

  public static JsonArray jsonResp(List<String> li) {
    JsonArray ja = new JsonArray();
    for (String s : li) {
      ja.add(jsonResp(s));
    }
    return ja;
  }

  public static JsonObject jsonResp(int code, String data) {
    JsonObject json = new JsonObject();
    json.addProperty("code", code);
    json.addProperty("data", data);
    return json;
  }

  public static JsonObject jsonResp(String data) {
    int code = data == null ? 500 : 200;
    return jsonResp(code, data);
  }
}
