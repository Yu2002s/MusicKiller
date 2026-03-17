package xyz.jdynb.music.config

import androidx.navigation.NavDestination
import xyz.jdynb.music.R

//private val hideToolbarDestinations = setOf<Int>(
//  // R.id.homeFragment
//)

private val hideBottomNavDestinations = setOf<Int>(
  // R.id.homeFragment
)

//fun NavDestination.shouldShowToolbar(): Boolean {
//  return this.id !in hideToolbarDestinations
//}

fun NavDestination.shouldShowBottomNav(): Boolean {
  return this.id !in hideBottomNavDestinations
}