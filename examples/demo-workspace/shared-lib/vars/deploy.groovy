// Global shared-library step: deploy(env)
// Call from a Jenkinsfile as:  deploy('staging')
def call(String environment = 'staging') {
    echo "Deploying application to ${environment}"
    // real impl would run kubectl / helm / etc.
}
