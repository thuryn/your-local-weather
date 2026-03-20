package org.thosp.yourlocalweather_wearos.presentation

enum class CompanionAppStatus {
    CHECKING, // Zjišťujeme stav
    MISSING,  // Aplikace chybí -> ukážeme varování
    INSTALLED // Vše je OK -> ukážeme počasí
}