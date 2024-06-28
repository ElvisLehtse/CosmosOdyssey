This is the localhost version of the project CosmosOdyssey which uses PostgreSQL as its database.  
Setup:  
1. Configure "Postgre credentials.txt" to match your database port number, username and password.  
   Line 2 (port=5432) change "5432" to match the database' port number.  
   Line 4 (user=postgres) change "postgres" to match the database' user name.  
   Line 5 (pass=1245) change "1245" to match the database' password.  
2. Configure "Localhost port.txt" to match the default port of the local server.  
   Line 1 (port=8080) change "8080" to match the default port of the local server.  
3. Run the program using Intellij IDEA or similar IDE that will automatically build the Maven project.  
4. Type "http://localhost:8080/" (replace 8080 with your local server's port number) to a browser.
