package io.github.mal32.endergames;

public class MoreMath {
  // Why does Java havent build in this???
  public static double roundN(double value, int places) {
    double scale = Math.pow(10, places);
    return Math.round(value * scale) / scale;
  }
}
