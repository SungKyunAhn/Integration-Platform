package com.aimir.fep.command.ws.client;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.5.2
 * 2013-01-11T13:33:56.695+09:00
 * Generated source version: 2.5.2
 * 
 */
@WebServiceClient(name = "CommandWS", 
                  wsdlLocation = "http://localhost:8080/services/CommandWS?wsdl",
                  targetNamespace = "http://server.ws.command.fep.aimir.com/") 
public class CommandWS_Service extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://server.ws.command.fep.aimir.com/", "CommandWS");
    public final static QName CommandWSPort = new QName("http://server.ws.command.fep.aimir.com/", "CommandWSPort");
    static {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/services/CommandWS?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(CommandWS_Service.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "http://localhost:8080/services/CommandWS?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public CommandWS_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public CommandWS_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public CommandWS_Service() {
        super(WSDL_LOCATION, SERVICE);
    }
    

    /**
     *
     * @return
     *     returns CommandWS
     */
    @WebEndpoint(name = "CommandWSPort")
    public CommandWS getCommandWSPort() {
        return super.getPort(CommandWSPort, CommandWS.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CommandWS
     */
    @WebEndpoint(name = "CommandWSPort")
    public CommandWS getCommandWSPort(WebServiceFeature... features) {
        return super.getPort(CommandWSPort, CommandWS.class, features);
    }

}
