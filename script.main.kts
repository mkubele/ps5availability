#!/usr/bin/env kscript

@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jsoup:jsoup:1.11.3")
@file:DependsOn("com.slack.api:slack-api-client:1.5.2")
@file:DependsOn("org.slf4j:slf4j-nop:1.7.30")

import com.slack.api.Slack
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

require(args.size == 1) { "Unexpected number of parameters - ${args.size}" }
val webhookUrl = args.first()

val availableString = "available!!!"
val notAvailableString = "not available"

fun datart() = commonListFun(Shop.DATART) { getDatartElement().getElementsByClass("orange") }
fun planeo() = commonListFun(Shop.PLANEO) { getPlaneElement().getElementsByClass("c-text-mute") }

fun getDatartElement(): Element = commonGetFirstElement(Shop.DATART, "product-detail-availability-box")
fun getPlaneElement(): Element = commonGetFirstElement(Shop.PLANEO, "book-availability")

fun commonGetFirstElement(shop: Shop, classElement: String): Element {
    val page = Jsoup.connect(shop.url).get().body()
    val el = page.getElementsByClass(classElement)

    return el.first()
}

fun commonListFun(shop: Shop, getFun: (shop: Shop) -> Elements) {
    val availability = getFun(shop)

    if (availability.size > 0) {
        println(notAvailableString)
    } else {
        success(shop)
    }
}

fun alza() {
    val availability = getAlzaText()

    if (availability.startsWith("Skladem")) {
        success(Shop.ALZA)
    } else {
        println(notAvailableString)
    }
}

fun getAlzaText(): String {
    val elements = getElementsFor(Shop.ALZA, "detail-page")

    return elements.first().getElementsByClass("stcStock").text()
}

fun czc() {
    val availability = getCzcText()

    if (availability.isNotBlank()) {
        if (!availability.startsWith("Skladem 0")) {
            success(Shop.CZC)
        } else {
            println(notAvailableString)
        }
    }
}

fun getCzcText(): String = getElementsFor(Shop.CZC, "stores-and-delivery").text()

fun getElementsFor(shop: Shop, className: String): Elements {
    val page = Jsoup.connect(shop.url).get().body()

    return page.getElementsByClass(className)
}

fun success(shop: Shop) {
    println(availableString)
    slackNotify(shop)
}

fun slackNotify(shop: Shop) {
    val message = prepareMessage(shop)
    val payload = "{\"text\":\"$message\"}"

    val response = Slack.getInstance().send(webhookUrl, payload)
    if (response.code == 200) {
        println("notification sent")
    } else {
        println(response)
    }
}

fun prepareMessage(shop: Shop): String = "skladem na <${shop.url}|*$shop*>"

enum class Shop(val url: String) {
    ALZA("https://alza.cz/gaming/playstation-5"),
    CZC("https://www.czc.cz/snakebyte-nabijecka-twin-charge-5-cerna-ps5/306269/produkt"),
    DATART("https://www.datart.cz/herni-konzole-sony-playstation-5.html"),
    PLANEO("https://www.planeo.cz/katalog/1296905-sony-playstation-5-herni-konzole-playstation-5.html")
}

println("starting")
datart()
planeo()
alza()
czc()
println("done")