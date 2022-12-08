package app.controller

import lombok.Data

@Data
class DatabaseCheck (
    var moduleName: String,
    var dbRequired: Boolean,
)