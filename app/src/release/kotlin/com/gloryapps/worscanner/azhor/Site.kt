package com.gloryapps.worscanner.azhor

import com.gloryapps.worscanner.scanner.azhor.AzhorApi
import com.gloryapps.worscanner.scanner.azhor.HttpAzhorApi
import com.gloryapps.worscanner.ui.Built

/** The site a release links to and sends scans to, at the address the build was given. */
fun site(): AzhorApi = HttpAzhorApi(Built.AZHOR_URL)
