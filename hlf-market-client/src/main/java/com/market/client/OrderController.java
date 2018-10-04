package com.market.client;

//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Demonstrates how to set up RESTful API endpoints using Spring MVC
 */

@RestController
@RequestMapping(value = "/market/v1")
//@Api(tags = {"cars"})
public class OrderController  {

    

    @RequestMapping(value = "item",
            method = RequestMethod.POST,
            consumes = {"application/json", "application/xml"},
            produces = {"application/json", "application/xml"})
    @ResponseStatus(HttpStatus.CREATED)
  //  @ApiOperation(value = "Create a car resource.", notes = "Returns the URL of the new resource in the Location header.")
    public String createItem(@RequestBody Item item,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
      System.out.println("Order controller Car ID -> " + item.itemId);
    	return HFJavaSDKBasicExample.createItem(item);
       
    }

    @RequestMapping(value = "items",
            method = RequestMethod.GET,
            produces = {"application/json", "application/xml"})
    @ResponseStatus(HttpStatus.OK)
    //@ApiOperation(value = "Get a paginated list of all hotels.", notes = "The list is paginated. You can provide a page number (default 0) and a page size (default 100)")
    public
    @ResponseBody
    String itemList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return HFJavaSDKBasicExample.itemList();
        //return this.hotelService.getAllHotels(page, size);
    }

//    @RequestMapping(value = "/{id}",
//            method = RequestMethod.GET,
//            produces = {"application/json", "application/xml"})
//    @ResponseStatus(HttpStatus.OK)
//    @ApiOperation(value = "Get a single hotel.", notes = "You have to provide a valid hotel ID.")
//    public
//    @ResponseBody
//    Hotel getHotel(@ApiParam(value = "The ID of the hotel.", required = true)
//                             @PathVariable("id") Long id,
//                             HttpServletRequest request, HttpServletResponse response) throws Exception {
//        //Hotel hotel = this.hotelService.getHotel(id);
//        //checkResourceFound(hotel);
//        //todo: http://goo.gl/6iNAkz
//        //return hotel;
//    }
//
//    @RequestMapping(value = "/{id}",
//            method = RequestMethod.PUT,
//            consumes = {"application/json", "application/xml"},
//            produces = {"application/json", "application/xml"})
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @ApiOperation(value = "Update a hotel resource.", notes = "You have to provide a valid hotel ID in the URL and in the payload. The ID attribute can not be updated.")
//    public void updateHotel(@ApiParam(value = "The ID of the existing hotel resource.", required = true)
//                                 @PathVariable("id") Long id, @RequestBody Hotel hotel,
//                                 HttpServletRequest request, HttpServletResponse response) {
//        checkResourceFound(this.hotelService.getHotel(id));
//        if (id != hotel.getId()) throw new DataFormatException("ID doesn't match!");
//        this.hotelService.updateHotel(hotel);
//    }
//
//    //todo: @ApiImplicitParams, @ApiResponses
//    @RequestMapping(value = "/{id}",
//            method = RequestMethod.DELETE,
//            produces = {"application/json", "application/xml"})
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @ApiOperation(value = "Delete a hotel resource.", notes = "You have to provide a valid hotel ID in the URL. Once deleted the resource can not be recovered.")
//    public void deleteHotel(@ApiParam(value = "The ID of the existing hotel resource.", required = true)
//                                 @PathVariable("id") Long id, HttpServletRequest request,
//                                 HttpServletResponse response) {
//        checkResourceFound(this.hotelService.getHotel(id));
//        this.hotelService.deleteHotel(id);
//    }
}
