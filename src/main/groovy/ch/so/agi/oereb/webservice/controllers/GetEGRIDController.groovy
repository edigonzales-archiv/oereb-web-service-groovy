package ch.so.agi.oereb.webservice.controllers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ch.admin.geo.schemas.v_d.oereb._1_0.extract.GetEGRIDResponseType
import ch.so.agi.oereb.webservice.services.GetEGRIDResponseTypeService

@RestController
class GetEGRIDController {
    private final Logger log = LoggerFactory.getLogger(this.getClass())
    
    @Autowired
    private Environment env

    @Autowired
    private GetEGRIDResponseTypeService getEGRIDResponseTypeService
    
    private defaultWebGISClientURL = "https://geo.so.ch/map/?bl=hintergrundkarte_sw"

    @RequestMapping(value="/getegrid/{format:xml|html}/", method=RequestMethod.GET, 
        produces = MediaType.APPLICATION_XML_VALUE,
        params = "XY")
    public ResponseEntity<?> getEgridByXY (
        @PathVariable("format") String format, 
        @RequestParam(value = "XY") String xy) {
        double[] coords = validateCoordinateRequestParam(xy)        
        GetEGRIDResponseType getEGRIDResponseType = getEGRIDResponseTypeService.getGetEGRIDResponseTypeByXY(coords[0], coords[1])
        
        if (getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 0) {
            log.warn("No egrid found at: " + xy);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }
        
        // TODO: When more than one egrid is found, we can return a html list with links.
        if (format.equalsIgnoreCase("html") && getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 3) {
            HttpHeaders headers = this.redirectHeaders(getEGRIDResponseType)
            return new ResponseEntity<Void>(headers, HttpStatus.FOUND)
        } 
        
        return ResponseEntity.ok(getEGRIDResponseType);
    }
    
    @RequestMapping(value="/getegrid/{format:xml|html}/", method=RequestMethod.GET,
        produces = MediaType.APPLICATION_XML_VALUE,
        params = "GNSS")
    public ResponseEntity<?> getEgridByGNSS (
        @PathVariable("format") String format, 
        @RequestParam(value = "GNSS") String gnss) {
        double[] coords = validateCoordinateRequestParam(gnss)
        GetEGRIDResponseType getEGRIDResponseType = getEGRIDResponseTypeService.getGetEGRIDResponseTypeByGNSS(coords[0], coords[1])
        
        if (getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 0) {
            log.warn("No egrid found at: " + gnss);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }
        
        if (format.equalsIgnoreCase("html") && getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 3) {
            HttpHeaders headers = this.redirectHeaders(getEGRIDResponseType)
            return new ResponseEntity<Void>(headers, HttpStatus.FOUND)
        }
        
        return ResponseEntity.ok(getEGRIDResponseType);
    }
    
    @RequestMapping(value="/getegrid/{format:xml}/{identdn:.{12,12}}/{number}", method=RequestMethod.GET,
        produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> getEgridByNumberAndIdentDN (
        @PathVariable("identdn") String identdn, 
        @PathVariable("number") String number) {            
        GetEGRIDResponseType getEGRIDResponseType = getEGRIDResponseTypeService.getGetEGRIDResponseTypeByNumberAndIdentDN(number, identdn)
    
        if (getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 0) {
            log.warn("No egrid found at: " + identdn + " / " + number)
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT)
        }
        
        return ResponseEntity.ok(getEGRIDResponseType)
    }
    
    @RequestMapping(value="/getegrid/{format:xml|json}/{postalcode:[0-9]{4,4}}/{localisation}/{number}", method=RequestMethod.GET,
        produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> getEgridByPostalcodeAndLocalisationAndNumber (
        @PathVariable("postalcode") String postalcode,
        @PathVariable("localisation") String localisation,
        @PathVariable("number") String number) {
        GetEGRIDResponseType getEGRIDResponseType = getEGRIDResponseTypeService.getGetEGRIDResponseTypeByPostalcodeAndLocalisationAndNumber(postalcode, localisation, number)
    
        if (getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 0) {
            log.warn("No egrid found at: " + postalcode + " / " + localisation + " / " + number)
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT)
        }
    
        return ResponseEntity.ok(getEGRIDResponseType)
    }
    
    @RequestMapping(value="/getegrid/{format:xml|json}/{postalcode:[0-9]{4,4}}/{localisation}", method=RequestMethod.GET,
        produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> getEgridByPostalcodeAndLocalisation (
        @PathVariable("postalcode") String postalcode,
        @PathVariable("localisation") String localisation) {
        GetEGRIDResponseType getEGRIDResponseType = getEGRIDResponseTypeService.getGetEGRIDResponseTypeByPostalcodeAndLocalisation(postalcode, localisation)
    
        if (getEGRIDResponseType.getEgridAndNumberAndIdentDN().size() == 0) {
            log.warn("No egrid found at: " + postalcode + " / " + localisation)
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT)
        }
    
        return ResponseEntity.ok(getEGRIDResponseType)
    }

    // Create the headers ('Location') for redirecting to the web gis client.
    private HttpHeaders redirectHeaders(GetEGRIDResponseType getEGRIDResponseType) {
        String egrid = getEGRIDResponseType.getEgridAndNumberAndIdentDN().getAt(0).value.toString()
        
        String url = env.getProperty("oereb.webgisclient.url", defaultWebGISClientURL)
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new String(url + "&st="+egrid+"&t=default").toURI())

        return headers
    }
    
    // For both coordinate request methods (XY, GNSS),
    // the request parameter must be two doubles, separated with a comma.
    private double[] validateCoordinateRequestParam(String coordinate) {
        String[] parts = coordinate.split(",")
        assert parts.length == 2
        
        assert parts[0].isDouble()
        assert parts[1].isDouble()
        
        double[] coords = new double[2]
        coords[0] = parts[0] as Double
        coords[1] = parts[1] as Double

        return coords
    }

    // This handles all exceptions that are in the @ExceptionHandler.
    // It will throw a 500 status code.
    @ExceptionHandler([org.codehaus.groovy.runtime.powerassert.PowerAssertionError.class, IllegalArgumentException.class, NumberFormatException.class])
    private ResponseEntity<?> handleBadRequests(Exception e) {
        log.error(e.getMessage());
        return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
