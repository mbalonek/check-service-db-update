package de.balonek.checkservicedbupdate.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import de.balonek.model.CheckResult
import de.balonek.model.DeliveryNote
import de.balonek.model.ModuleReleaseCheckResult
import org.apache.http.client.utils.URIBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.logging.Logger


@Service
@Configuration
class ReleaseLoader (
    var releases : MutableMap<String, Boolean>,
    var env: Environment,
    var kafkaTemplate : KafkaTemplate<String, String>
        ) {
    val log = Logger.getLogger("ReleaseLoader")


    fun loadReleaseFiles(deliveryNote: DeliveryNote) {

        val modulesAmount = deliveryNote.modulesReleases.size

        val client: HttpClient = HttpClient.newBuilder().build()

        val uriBuilder = URIBuilder()
        uriBuilder.setScheme(env.getProperty("repository.protocol"))
            .setHost(env.getProperty("repository.name"))
            .setPathSegments(env.getProperty("repository.version"),
                        env.getProperty("repository.download"),
                        env.getProperty("repository.action"))


        for (i in 1..modulesAmount) {
            val request = HttpRequest.newBuilder()
                .uri(uriBuilder.build())
                .GET()
                .headers(
                    "Authorization",
                    env.getProperty("repository.api.token")
                )
                .headers(env.getProperty("repository.api.endpoint"), "{\"path\":\"/releases/release-module_"+i+".csv\"}")
                .build()

            val response : ByteArray? = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body()

            getModulesDetails(response)
        }

    }

    fun getModulesDetails(content: ByteArray?) {

        val inputStream: InputStream = ByteArrayInputStream(content)

        val reader = inputStream.bufferedReader()
        //val header = reader.readLine()

        reader.lineSequence()
            .filter { it.isNotBlank() }
            .forEach {releases.put(it.substringBefore(","), (it.substringAfter(",").substringBefore(",")).toBoolean()) }

    }

    fun compareReleaseAndDeliveryNoteDb(deliveryNote: DeliveryNote): CheckResult {


        val modulesDeliveryNote :MutableMap<String, Boolean> = mutableMapOf()

        deliveryNote.modulesReleases.forEach { module ->  modulesDeliveryNote.put(module.name, module.isDBSchemaActualisationRequired)}

        val sortedModulesOfDeliveryNote = modulesDeliveryNote.toSortedMap()

        val sortedModulesOfRelease = releases.toSortedMap()

        val difference :MapDifference<String, Boolean> = Maps.difference(sortedModulesOfDeliveryNote,sortedModulesOfRelease)



        val map: MutableMap<String, MapDifference.ValueDifference<Boolean>> = difference.entriesDiffering()

        val checkResult = CheckResult.builder().checkpoint(env.getProperty("microservice.endpoint")).build()

        val moduleReleaseCheckResult = ModuleReleaseCheckResult()

        map.forEach{ mod -> moduleReleaseCheckResult.moduleName = mod.key
                            moduleReleaseCheckResult.isCorrect = (mod.value.leftValue()==mod.value.rightValue())
                            moduleReleaseCheckResult.checkDetails = ("Delivery Note: "+ mod.value.leftValue() + " - Release "+mod.value.rightValue())
            checkResult.moduleReleasesCheckResults.add(moduleReleaseCheckResult)
        }

        checkResult.id= UUID.randomUUID().toString()

        return checkResult
    }

    suspend fun sendResults(checkResult: CheckResult) {
        val mapper = jacksonObjectMapper()
        val resultJSON = mapper.writeValueAsString(checkResult)
        kafkaTemplate.send("check-results",resultJSON)
        log.info("Check-results send : "+resultJSON)
/*
        val client: HttpClient = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()

           .uri(URI.create(env.getProperty("facade.base.url")))
           .header("Content-Type", "application/json")
           .POST(HttpRequest.BodyPublishers.ofString(jsonResults))
           .build()

       val response = client.send(request,HttpResponse.BodyHandlers.ofString()) //TODO ASYNC
       log.info("Send. ${HttpStatus.valueOf(response.statusCode()).reasonPhrase}")
       */

       // return HttpStatus.valueOf(response.statusCode())
   }
}