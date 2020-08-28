import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server

object RulesHelper {
    suspend fun getRulesMessage(server: Server): String? {
        return (server.textChannels.findByName("rules") ?: return null).getMessages().first().content
    }

    suspend fun getRules(server: Server): HashMap<String, String> {
        val rulesMap: HashMap<String, String> = HashMap()
        val noFormatRules = getRulesMessage(server) ?: return rulesMap
        val rulesByLine = noFormatRules.split("\n")
        var lastNumber = ""
        for (ruleLine in rulesByLine) {
            val ruleSplit = ruleLine.trim().split(" ", limit = 2)
            var ruleID = ruleSplit[0].replace("*", "").replace(".", "")
            if (ruleID.isEmpty()) continue
            try {
                Integer.parseInt(ruleID)
                lastNumber = ruleID
            } catch (_: NumberFormatException) {
                ruleID = "$lastNumber$ruleID"
            }
            rulesMap[ruleID] = ruleSplit[1]
        }
        return rulesMap
    }

    suspend fun getRule(server: Server, rule: String): String? {
        return getRules(server)[rule]
    }
}