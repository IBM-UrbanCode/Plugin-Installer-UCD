/**
 * © Copyright IBM Corporation 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.AirPluginTool
import com.urbancode.air.XTrustProvider
import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties()

def ucdServer  = props["ucdServer"].trim()
def username   = props["username"].trim()
def password   = props["password"]
def pluginStr  = props["pluginStr"].trim()
def autoOrSC   = props["autoOrSC"]
def acceptAllCerts  = Boolean.valueOf(props["acceptAllCerts"])

if (acceptAllCerts) {
    XTrustProvider.install()
}

URI uri = null
try {
    if (autoOrSC == "Automation") {
        uri = new URIBuilder(ucdServer).setPath("/rest/plugin/automationPlugin").build()
    } else {
        uri = new URIBuilder(ucdServer).setPath("/rest/plugin/sourceConfigPlugin").build()    
    }
}
catch (URISyntaxException e) {
    throw new URISyntaxException("[Error] Failed to construct a valid URI. Reason: " + e.getMessage())
}

File pluginZip = new File(pluginStr)
if (!pluginZip.isFile()) {
    throw new RuntimeException("[Error] Could not find specified '${pluginStr}' plugin zip.")
}

MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create()
multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
multipartEntity.addBinaryBody("file", pluginZip, ContentType.create("application/zip"), pluginZip.getName())
multipartEntity.addTextBody("filename", pluginZip.getName())

HttpClientBuilder clientBuilder = new HttpClientBuilder()
clientBuilder.setUsername(username)
clientBuilder.setPassword(password)
clientBuilder.setPreemptiveAuthentication(true)
CloseableHttpClient client = clientBuilder.buildClient()

HttpPost httppost = new HttpPost(uri)
httppost.setEntity(multipartEntity.build())
CloseableHttpResponse response = response = client.execute(httppost)
def statusCode
def statusLine
try {
    statusCode = response.getStatusLine().getStatusCode()
    statusLine = response.getStatusLine().toString()
} finally {
    response.close()
}

if (statusCode < 200 || statusCode >= 300) {
    throw new RuntimeException("[Error] Failed to upload plugin: " + statusCode + " - " + statusLine)
}

println "[OK] '${pluginZip.getName()}' succesfully installed into the '${ucdServer}' IBM UrbanCode Deploy server."
