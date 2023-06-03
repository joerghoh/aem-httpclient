
# Quickstart


## Adding the reference

This quickstart assumes that you are using an AEM project which has been bootstraped with the AEM archetype (the version does not matter).

In order to include the aem-httpclient library into your AEM deployment, you need to change 3 distinct pom.xml files. 


### pom.xml

in the pom.xml in the root folder you add the aem-httpclient into the ``dependencyManagement`` section:

```
<dependencyManagement>
...
  <dependency>
    <groupId>de.joerghoh</groupId>
    <artifactId>aem-httpclient</artifactId>
    <version>0.2</version>
    <scope>provided</scope>
  </dependency>
...
</dependencyManagement>
```

If you update the version of the aem-httpclient you need to change the version number only here.


### all/pom.xml

In the ``all`` module you need to configure the ``aem-httpclient`` to get embedded into your content-package.

```
<plugin>
  <artifactId>filevault-package-maven-plugin</artifactId>
  ...
  <configuration>
    <embeddeds>
      <embedded>
        <groupId>de.joerghoh</groupId>
        <artifactId>aem-httpclient</artifactId>
        <target>/apps/<yourapp>/application/install</target>
      <embedded>
    </embeddeds>
  </configuration>
</plugin>
```
Replace ``<yourapp>`` with a path in your application; in the end the aem-httpclient bundle should get deployed next to your ``core`` bundle.


### core/pom.xml

In the core module you need to define the aem-httpclient as simple dependency, so you can use its API.

```
<dependencies>
...
  <dependency>
    <groupId>de.joerghoh</groupId>
    <artifactId>aem-httpclient</artifactId>
  </dependency>
...
</dependencies>
```


## OSGI configuration

The aem-httpclient does not start a httpclient by default, you need to explicitly request it. Do this by requesting a service instance with the name "default".

In your ``ui.config`` module, create in the ``src/main/content/jcr_root/apps/<yourapp>/osgiconfig/config`` folder a file with the name ``de.joerghoh.aem.httpclient.impl.HttpClientImpl-default.cfg.json`` with the following content:

```
{
  "id" : "default"
}
```

This will instantiate a service with the id "default", and which is using reasonable default settings.


## Using the aem-httpclient


Now you can start using your configured httpclient. You can use it like this:


```
import de.joerghoh.aem.httpclient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
...


@Reference(target="(id=default)")
HttpClient httpclient;

...

private String getGoogleHomepageContent() {
  SimpleHttpRequest googleRequest = new SimpleHttpRequest("GET", "https://www.google.com");
  String googleContent = httpclient.performRequest(googleRequest, 
    (response) -> {
      return response.getBodyText();
    	}, 
    	(exception) -> {
    	  return "exception: " + exception.getMessage();
    	);
   resp.getWriter().write(googleContent);
  }
```

Notes:
* The ``@Reference`` annotation explicitly lists a specific service instance which should be injected. This is the ``default`` service we instantiated before. When only 1 instance of the httpclient is being created, this is not needed, but you should explicitly state which instance you want.


Caveats:
* This approach is not suited for large responses (> 1MB), as it buffers the complete response in heap; if you need to handle potentially larger responses, you should use a streaming approach described at tbd.











