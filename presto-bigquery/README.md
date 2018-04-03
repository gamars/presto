1. **download**: https://storage.googleapis.com/simba-bq-release/jdbc/SimbaJDBCDriverforGoogleBigQuery42_1.1.5.1005.zip
2. **expand**: `unzip SimbaJDBCDriverforGoogleBigQuery42_1.1.5.1005.zip`
3. **install**: `mvn install:install-file -Dfile=GoogleBigQueryJDBC42.jar -DgroupId=com.simba -DartifactId=bigquery-jdbc -Dversion=1.1.5.1005 
-Dpackaging=jar`
4. build from root
5. set ENV `GOOGLE_APPLICATION_CREDENTIALS` to .json or p12 key for service account
6. run
