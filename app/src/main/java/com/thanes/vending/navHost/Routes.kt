package com.thanes.vending.navHost

sealed  class Routes(val route: String) {
  object Login: Routes(route = "login_route")
  object Home: Routes(route = "home_route")
}