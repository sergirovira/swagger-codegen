import com.wordnik.swagger.core.util.JsonUtil
import com.wordnik.swagger.core.{Documentation, DocumentationSchema}

import com.wordnik.swagger.codegen.{BasicObjcGenerator, Codegen}
import com.wordnik.swagger.codegen.util._
import com.wordnik.swagger.codegen.language._
import com.wordnik.swagger.codegen.PathUtil

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class BasicObjcGeneratorTest extends FlatSpec with ShouldMatchers {
  val json = ScalaJsonUtil.getJsonMapper

  val config = new BasicObjcGenerator

  behavior of "BasicObjcGenerator"
  /*
   * A response of type "void" will turn into a declaration of None
   * for the template generator
   */
  it should "process a response declaration" in {
  	config.processResponseDeclaration("void") should be (Some("void"))
  }

  /*
   * swagger strings are turned into Objective-C NSString*
   */
  it should "process a string response" in {
  	config.processResponseDeclaration("string") should be (Some("NSString*"))
  }

  /*
   * swagger int is turned into Objective-c Int
   */
  it should "process an unmapped response type" in {
  	config.processResponseDeclaration("int") should be (Some("NSNumber*"))
  }

  /*
   * returns the invoker package from the config
   */
  it should "get the invoker package" in {
  	config.invokerPackage should be (None)
  }

  /*
   * returns the api package
   */
  it should "get the api package" in {
  	config.apiPackage should be (None)
  }

  /*
   * returns the model package
   */
  it should "get the model package" in {
  	config.modelPackage should be (None)
  }

  /*
   * types are mapped between swagger types and language-specific
   * types
   */
  it should "convert to a declared type" in {
  	config.toDeclaredType("string") should be ("NSString*")
    config.toDeclaredType("int") should be ("NSNumber*")
    config.toDeclaredType("float") should be ("NSNumber*")
    config.toDeclaredType("long") should be ("NSNumber*")
    config.toDeclaredType("double") should be ("NSNumber*")
    config.toDeclaredType("object") should be ("NSObject*")
    config.toDeclaredType("User") should be ("NIKUser*")
  }

  /*
   * declarations are used in models, and types need to be
   * mapped appropriately
   */
  it should "convert a string a declaration" in {
    val expected = Map("string" -> ("NSString*", "null"),
      "int" -> ("NSNumber*", "null"),
      "float" -> ("NSNumber*", "null"),
      "long" -> ("NSNumber*", "null"),
      "double" -> ("NSNumber*", "null"),
      "object" -> ("NSObject*", "null"))
    expected.map(e => {
      val model = new DocumentationSchema
      model.name = "simple_" + e._1
      model.setType(e._1)
      config.toDeclaration(model) should be (e._2)
    })
  }

  /*
   * codegen should honor special imports to avoid generating
   * classes
   */
  it should "honor the import mapping" in {
  	config.importMapping("Date") should be ("NIKDate")
  }

  /*
   * single tick reserved words
   */
  it should "quote a reserved var name" in {
  	config.toVarName("char") should be ("_char")
  }

  /*
   * support list declarations with string inner value and the correct default value
   */
   it should "create a declaration with a List of strings" in {
      val model = new DocumentationSchema
      model.name = "arrayOfStrings"
      model.setType("Array")
      model.items = new DocumentationSchema
      model.items.setType("string")

      val m = config.toDeclaration(model)
      m._1 should be ("NSArray*")
      m._2 should be ("null")
   }

  /*
   * support list declarations with int inner value and the correct default value
   */
   it should "create a declaration with a List of ints" in {
      val model = new DocumentationSchema
      model.name = "arrayOfInts"
      model.setType("Array")
      model.items = new DocumentationSchema
      model.items.setType("int")

      val m = config.toDeclaration(model)
      m._1 should be ("NSArray*")
      m._2 should be ("null")
   }

  /*
   * support list declarations with float inner value and the correct default value
   */
   it should "create a declaration with a List of floats" in {
      val model = new DocumentationSchema
      model.name = "arrayOfFloats"
      model.setType("Array")
      model.items = new DocumentationSchema
      model.items.setType("float")

      val m = config.toDeclaration(model)
      m._1 should be ("NSArray*")
      m._2 should be ("null")
   }

  /*
   * support list declarations with double inner value and the correct default value
   */
   it should "create a declaration with a List of doubles" in {
      val model = new DocumentationSchema
      model.name = "arrayOfDoubles"
      model.setType("Array")
      model.items = new DocumentationSchema
      model.items.setType("double")

      val m = config.toDeclaration(model)
      m._1 should be ("NSArray*")
      m._2 should be ("null")
   }

  /*
   * support list declarations with complex inner value and the correct default value
   */
   it should "create a declaration with a List of complex objects" in {
      val model = new DocumentationSchema
      model.name = "arrayOfFloats"
      model.setType("Array")
      model.items = new DocumentationSchema
      model.items.setType("User")

      val m = config.toDeclaration(model)
      m._1 should be ("NSArray*")
      m._2 should be ("null")
   }

   it should "verify an api map with path param" in {
    val resourceListing = json.readValue(ResourceExtractor.extractListing("src/test/resources/petstore/resources.json", None), classOf[Documentation])

    val subDocs = ApiExtractor.extractApiDocs("src/test/resources/petstore", resourceListing.getApis.asScala.toList)
    val codegen = new Codegen(config)
    val petApi = subDocs.filter(doc => doc.getResourcePath == "/pet").head

    val endpoint = petApi.getApis().asScala.filter(api => api.path == "/pet.{format}/{petId}").head
    val operation = endpoint.getOperations.asScala.filter(op => op.httpMethod == "GET").head
    val m = codegen.apiToMap("http://my.api.com/api", operation)

    m("path") should be ("http://my.api.com/api")
    m("bodyParams").asInstanceOf[List[_]].size should be (0)
    m("httpMethod") should be ("GET")
    // Pet => NIKPet
    m("returnBaseType") should be (Some("NIKPet"))
    m("returnTypeIsPrimitive") should be (None)
    m("pathParams").asInstanceOf[List[_]].size should be (1)

    val idParam = m("pathParams").asInstanceOf[List[_]].head.asInstanceOf[HashMap[String, _]]
    idParam("paramName") should be ("petId")
    idParam("dataType") should be ("NSString*")
    idParam("required") should be ("true")
    idParam("swaggerDataType") should be ("string")
    idParam("baseName") should be ("petId")
    idParam("type") should be ("path")
    idParam("allowMultiple") should be ("false")
    idParam("defaultValue") should be (None)
  }

  it should "verify an api map with query params" in {
    val resourceListing = json.readValue(ResourceExtractor.extractListing("src/test/resources/petstore/resources.json", None), classOf[Documentation])

    val subDocs = ApiExtractor.extractApiDocs("src/test/resources/petstore", resourceListing.getApis.asScala.toList)
    val codegen = new Codegen(config)
    val petApi = subDocs.filter(doc => doc.getResourcePath == "/pet").head

    val endpoint = petApi.getApis().asScala.filter(api => api.path == "/pet.{format}/findByTags").head
    val operation = endpoint.getOperations.asScala.filter(op => op.httpMethod == "GET").head
    val m = codegen.apiToMap("http://my.api.com/api", operation)

    m("path") should be ("http://my.api.com/api")
    m("bodyParams").asInstanceOf[List[_]].size should be (0)
    m("httpMethod") should be ("GET")

    // Pet => NIKPet
    m("returnBaseType") should be (Some("NIKPet"))
    m("returnType") should be (Some("NSArray*"))
    m("returnTypeIsPrimitive") should be (None)
    m("pathParams").asInstanceOf[List[_]].size should be (0)
    m("returnContainer") should be ("List")
    m("requiredParamCount") should be ("1")

    val queryParams = m("queryParams").asInstanceOf[List[_]]
    queryParams.size should be (1)

    val queryParam = queryParams.head.asInstanceOf[HashMap[String, _]]
    queryParam("type") should be ("query")
    queryParam("dataType") should be ("NSString*")
    queryParam("required") should be ("true")
    queryParam("paramName") should be ("tags")
    queryParam("swaggerDataType") should be ("string")
    queryParam("allowMultiple") should be ("true")
  }

  it should "create an api file" in {
    val codegen = new Codegen(config)
    val resourceListing = json.readValue(ResourceExtractor.extractListing("src/test/resources/petstore/resources.json", None), classOf[Documentation])

    val subDocs = ApiExtractor.extractApiDocs("src/test/resources/petstore", resourceListing.getApis.asScala.toList)
    val petApi = subDocs.filter(doc => doc.getResourcePath == "/pet").head

    val endpoint = petApi.getApis().asScala.filter(api => api.path == "/pet.{format}/findByTags").head
    val operation = endpoint.getOperations.asScala.filter(op => op.httpMethod == "GET").head
    val m = codegen.apiToMap("http://my.api.com/api", operation)

    implicit val basePath = "http://localhost:8080/api"

    val allModels = new HashMap[String, DocumentationSchema]
    val operations = config.extractOperations(subDocs, allModels)

    val apiMap = config.groupApisToFiles(operations)
    val bundle = config.prepareApiBundle(apiMap)
    val apiFiles = config.bundleToSource(bundle, config.apiTemplateFiles.toMap)

    apiFiles.size should be (6)
  }
}