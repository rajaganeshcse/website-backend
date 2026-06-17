package com.portfolio.backend.util;

import com.portfolio.backend.model.Hero;

public final class Defaults {

  private Defaults() {
  }

  public static Hero defaultHero() {
    Hero hero = new Hero();
    hero.name = "Rajaganesh T";
    hero.title = "Aspiring Android App Developer";
    hero.bio = "Passionate and detail-oriented developer building Android and mobile applications with Java, Dart, Android Studio, Firebase, React, and MySQL.";
    hero.email = "rajaganeshcse2005@gmail.com";
    hero.phone = "+91 6382641748";
    hero.github = "https://github.com/rajaganeshcse";
    hero.linkedin = "https://www.linkedin.com/in/rajaganesh-t-835a21364";
    hero.leetcode = "";
    hero.instagram = "";
    hero.portfolio = "";
    hero.location = "Vanrasankuppam, Cuddalore, Tamil Nadu, India";
    hero.college = "Dhanalakshmi Srinivasan Engineering College, Perambalur - B.E CSE (2023 - 2027)";
    hero.address = "70, MaariAmman Kovil Street, Vanrasankuppam, Cuddalore (TK), Cuddalore (DT) - 607102";
    hero.photo_path = "";
    hero.site_icon_path = "";
    hero.resume_path = "";
    hero.resume_name = "";
    return hero;
  }
}
