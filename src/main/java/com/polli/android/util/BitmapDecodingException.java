package com.polli.android.util;

public class BitmapDecodingException extends Exception {

  public BitmapDecodingException(String s) {
    super(s);
  }

  public BitmapDecodingException(Exception nested) {
    super(nested);
  }
}
