package tech.skot.libraries.security

interface SecurityActions {
    fun getBioAuthentAvailability(
        onResult: (availability: BioAuthentAvailability) -> Unit,
    )

    fun doWithBioAuthent(title:String, subTitle:String? = null, onKo:(()->Unit)? = null, onOk:()->Unit)

    fun enrollBioAuthent()

    fun encodeWithBioAuthent(keyName:String, strData:String):String

    fun decodeWithBioAuthent(keyName:String, strData:String):String
}