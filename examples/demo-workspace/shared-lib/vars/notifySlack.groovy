// Global shared-library step: notifySlack(channel, message)
def call(String channel, String message) {
    echo "Slack #${channel}: ${message}"
}
