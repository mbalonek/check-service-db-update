package de.balonek.checkservicedbupdate.controller

import de.balonek.checkservicedbupdate.service.ReleaseLoader
import de.balonek.model.DeliveryNote
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.*

@RestController
class CheckController(
    var releaseLoader: ReleaseLoader
) {

    @GetMapping("/check")
    suspend fun checkDatabaseRequirements(@RequestBody deliveryNote: DeliveryNote) : ResponseEntity<String>{

        releaseLoader.loadReleaseFiles(deliveryNote)
        val checkResult = releaseLoader.compareReleaseAndDeliveryNoteDb(deliveryNote)
        checkResult.checkId=deliveryNote.checkId
        checkResult.sprint=deliveryNote.sprintNumber
        checkResult.timestamp=LocalDateTime.now().toString()

        releaseLoader.sendResults(checkResult)

        return ResponseEntity.ok().body("Received")
    }

}