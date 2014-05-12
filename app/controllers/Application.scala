package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws.WS 
import play.api.libs.ws.Response
import play.api.libs.iteratee.Enumerator
import java.net.URL 
import play.api.libs.concurrent.Execution.Implicits.defaultContext


// https://github.com/rm-hull/kebab/blob/master/app/Proxy.scala
// http://localhost:9000/
// http://www.playframework.com/documentation/2.2.x/Iteratees


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def proxy = Action.async {
    request =>  
      val proxyUrl = urlToProxy(request.uri) 
      val response = WS.url(proxyUrl).execute(request.method) 
      Async { 
        response.map { 
          case response : Response => getStream(response) 
          case _ => RequestTimeout("Timed out")  
        }
      }  
      /*
      val dataFuture = getStream(response)
      val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 1.second)
      Future.firstCompletedOf(Seq(dataFuture, timeoutFuture)).map {
        case r: play.api.libs.ws.Response => r 
        case t: String => InternalServerError(t)
      }

     //  val data = response.ahcResponse.getResponseBodyAsStream
     //  val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(data)
     //  Ok.chunked(dataContent)    
      //Ok.stream(rawData(request.uri))
      //Ok(views.html.index("Your new application is ready."))
      */
  }


  private def urlToProxy(url : String) : String = {
    var u = new URL(url) 
    u.set( u.getProtocol, u.getHost, 5984, u.getAuthority, u.getUserInfo, u.getPath, u.getQuery, u.getRef) ;
    u.toString() ; 
  } 


  private def getStream(response : Response) = {
    val data = response.ahcResponse.getResponseBodyAsStream
    val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(data)
    Status(response.status)
      .stream(dataContent)
      .as(response.header(CONTENT_TYPE).getOrElse("plain/text"))
  }


}
