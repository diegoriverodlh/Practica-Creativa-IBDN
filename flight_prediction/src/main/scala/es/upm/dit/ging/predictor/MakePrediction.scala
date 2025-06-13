package es.upm.dit.ging.predictor

import com.mongodb.spark._
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.ml.feature.{Bucketizer, StringIndexerModel, VectorAssembler}
import org.apache.spark.sql.functions.{concat, from_json, lit, to_json, struct}
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{SparkSession}

object MakePrediction {

  def main(args: Array[String]): Unit = {
    println("Flight predictor starting...")

    val spark = SparkSession
      .builder
      .appName("StructuredNetworkWordCount")
      .master("spark://spark-master:7077")
      .getOrCreate()
    import spark.implicits._

    val base_path = "/practica_creativa"
    val arrivalBucketizerPath = s"$base_path/models/arrival_bucketizer_2.0.bin"
    val arrivalBucketizer = Bucketizer.load(arrivalBucketizerPath)

    val columns = Seq("Carrier", "Origin", "Dest", "Route")

    val stringIndexerModelPaths = columns.map(col => s"$base_path/models/string_indexer_model_$col.bin")
    val stringIndexerModels = (columns zip stringIndexerModelPaths.map(StringIndexerModel.load)).toMap

    val vectorAssemblerPath = s"$base_path/models/numeric_vector_assembler.bin"
    val vectorAssembler = VectorAssembler.load(vectorAssemblerPath)

    val randomForestModelPath = s"$base_path/models/spark_random_forest_classifier.flight_delays.5.0.bin"
    val rfc = RandomForestClassificationModel.load(randomForestModelPath)

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("subscribe", "flight-delay-ml-request")
      .load()

    val flightJsonDf = df.selectExpr("CAST(value AS STRING)")

    val struct = new StructType()
      .add("Origin", DataTypes.StringType)
      .add("FlightNum", DataTypes.StringType)
      .add("DayOfWeek", DataTypes.IntegerType)
      .add("DayOfYear", DataTypes.IntegerType)
      .add("DayOfMonth", DataTypes.IntegerType)
      .add("Dest", DataTypes.StringType)
      .add("DepDelay", DataTypes.DoubleType)
      .add("Prediction", DataTypes.StringType)
      .add("Timestamp", DataTypes.TimestampType)
      .add("FlightDate", DataTypes.DateType)
      .add("Carrier", DataTypes.StringType)
      .add("UUID", DataTypes.StringType)
      .add("Distance", DataTypes.DoubleType)

    val flightNestedDf = flightJsonDf.select(from_json($"value", struct).as("flight"))

    val flightFlattenedDf = flightNestedDf.selectExpr(
      "flight.Origin", "flight.DayOfWeek", "flight.DayOfYear", "flight.DayOfMonth",
      "flight.Dest", "flight.DepDelay", "flight.Timestamp", "flight.FlightDate",
      "flight.Carrier", "flight.UUID", "flight.Distance"
    )

    val predictionRequestsWithRoute = flightFlattenedDf.withColumn(
      "Route", concat($"Origin", lit("-"), $"Dest")
    )

    // Aplicar los StringIndexerModel
    val withStringIndexes = stringIndexerModels.foldLeft(predictionRequestsWithRoute) {
      case (df, (colName, model)) => model.transform(df)
    }

    // Vectorizar columnas numéricas
    val vectorizedFeatures = vectorAssembler.setHandleInvalid("keep").transform(withStringIndexes)

    val finalVectorizedFeatures = vectorizedFeatures
      .drop("Carrier_index")
      .drop("Origin_index")
      .drop("Dest_index")
      .drop("Route_index")

    val predictions = rfc.transform(finalVectorizedFeatures)
      .drop("Features_vec")
      .drop("indices").drop("values").drop("rawPrediction").drop("probability")

    val finalPredictions = predictions

    // MongoDB writer
    val dataStreamWriter = finalPredictions
      .writeStream
      .format("mongodb")
      .option("spark.mongodb.connection.uri", "mongodb://mongo:27017")
      .option("spark.mongodb.database", "agile_data_science")
      .option("spark.mongodb.collection", "flight_delay_ml_response")
      .option("checkpointLocation", "/tmp/mongo-checkpoint")
      .outputMode("append")

    // Kafka writer
    val kafkaWriter = finalPredictions
      .selectExpr("CAST(UUID AS STRING) AS key", "to_json(struct(*)) AS value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("topic", "flight-delay-classification-response")
      .option("checkpointLocation", "/tmp/kafka-checkpoint")
      .outputMode("append")

    // HDFS writer (Parquet)
    val hdfsWriter = finalPredictions
      .writeStream
      .format("parquet")
      .option("path", "hdfs://namenode1:8020/user/root/flight_delay_ml_response")
      .option("checkpointLocation", "/tmp/hdfs-checkpoint")
      .outputMode("append")

    // Console output
    val consoleOutput = finalPredictions.writeStream
      .outputMode("append")
      .format("console")
      .start()

    // Start all writers
    val queryMongo = dataStreamWriter.start()
    val queryKafka = kafkaWriter.start()
    val queryHDFS = hdfsWriter.start()

    queryMongo.awaitTermination()
    queryKafka.awaitTermination()
    queryHDFS.awaitTermination()
    consoleOutput.awaitTermination()
  }

}
