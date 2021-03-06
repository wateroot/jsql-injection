package com.jsql.model.injection.suspendable;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.jsql.exception.PreparationException;
import com.jsql.exception.StoppableException;
import com.jsql.model.injection.MediatorModel;
import com.jsql.model.vendor.CubridStrategy;
import com.jsql.model.vendor.DB2Strategy;
import com.jsql.model.vendor.DerbyStrategy;
import com.jsql.model.vendor.FirebirdStrategy;
import com.jsql.model.vendor.H2Strategy;
import com.jsql.model.vendor.HSQLDBStrategy;
import com.jsql.model.vendor.InformixStrategy;
import com.jsql.model.vendor.IngresStrategy;
import com.jsql.model.vendor.MariaDBStrategy;
import com.jsql.model.vendor.MaxDbStrategy;
import com.jsql.model.vendor.MySQLStrategy;
import com.jsql.model.vendor.OracleStrategy;
import com.jsql.model.vendor.PostgreSQLStrategy;
import com.jsql.model.vendor.SQLServerStrategy;
import com.jsql.model.vendor.SybaseStrategy;
import com.jsql.model.vendor.TeradataStrategy;
import com.jsql.model.vendor.Vendor;

/**
 * Runnable class, define insertionCharacter that will be used by all futures requests,
 * i.e -1 in "[..].php?id=-1 union select[..]", sometimes it's -1, 0', 0, etc,
 * this class/function tries to find the working one by searching a special error message
 * in the source page.
 */
public class SuspendableGetDbVendor extends AbstractSuspendable {
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getLogger(SuspendableGetDbVendor.class);

    @Override
    public String action(Object... args) throws PreparationException, StoppableException {

        if (MediatorModel.model().selectedVendor != Vendor.Undefined) {
            if (MediatorModel.model().selectedVendor == Vendor.Cubrid) {
                MediatorModel.model().sqlStrategy = new CubridStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.DB2) {
                MediatorModel.model().sqlStrategy = new DB2Strategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Derby) {
                MediatorModel.model().sqlStrategy = new DerbyStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Firebird) {
                MediatorModel.model().sqlStrategy = new FirebirdStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.H2) {
                MediatorModel.model().sqlStrategy = new H2Strategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.HSQLDB) {
                MediatorModel.model().sqlStrategy = new HSQLDBStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Informix) {
                MediatorModel.model().sqlStrategy = new InformixStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Ingres) {
                MediatorModel.model().sqlStrategy = new IngresStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.MariaDB) {
                MediatorModel.model().sqlStrategy = new MariaDBStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.MaxDb) {
                MediatorModel.model().sqlStrategy = new MaxDbStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.MySQL) {
                MediatorModel.model().sqlStrategy = new MySQLStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Oracle) {
                MediatorModel.model().sqlStrategy = new OracleStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.PostgreSQL) {
                MediatorModel.model().sqlStrategy = new PostgreSQLStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.SQLServer) {
                MediatorModel.model().sqlStrategy = new SQLServerStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Sybase) {
                MediatorModel.model().sqlStrategy = new SybaseStrategy();
            } else if (MediatorModel.model().selectedVendor == Vendor.Teradata) {
                MediatorModel.model().sqlStrategy = new TeradataStrategy();
            }
            return null;
        }
        
        // Parallelize the search and let the user stops the process if needed.
        // SQL: force a wrong ORDER BY clause with an inexistent column, order by 1337,
        // and check if a correct error message is sent back by the server:
        //         Unknown column '1337' in 'order clause'
        // or   supplied argument is not a valid MySQL result resource
        ExecutorService taskExecutor = Executors.newCachedThreadPool();
        CompletionService<CallableHTMLPage> taskCompletionService = new ExecutorCompletionService<CallableHTMLPage>(taskExecutor);
        for (String insertionCharacter : new String[] {"'\"#-)'\""}) {
            taskCompletionService.submit(
                new CallableHTMLPage(
                    insertionCharacter,
                    insertionCharacter
                )
            );
        }

        int total = 1;
        while (0 < total) {
            // The user need to stop the job
            /**
             * TODO pauseOnUserDemand()
             * stop()
             */
            if (this.shouldSuspend()) {
                throw new StoppableException();
            }
            try {
                CallableHTMLPage currentCallable = taskCompletionService.take().get();
                total--;
                String pageSource = currentCallable.getContent();
                
                if (pageSource.matches("(?si).*("
                        // JDBC + php : same error
                        + "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near"
                        + "|"
                        + "MySQL"
                + ").*")) {
                    MediatorModel.model().sqlStrategy = new MySQLStrategy();
//                    System.out.println("MySQLStrategy");
                }
                
                if (pageSource.matches("(?si).*("
                        // JDBC + php : same error
                        + "You have an error in your SQL syntax; check the manual that corresponds to your MariaDB server version for the right syntax to use near"
                        + "|"
                        + "MariaDB"
                        + ").*")) {
                    MediatorModel.model().sqlStrategy = new MariaDBStrategy();
//                    System.out.println("MariaDBStrategy");
                }
                
                if (pageSource.matches("(?si).*("
                        // JDBC + php : same error
                        + "HSQLDB"
                        + ").*")) {
                    MediatorModel.model().sqlStrategy = new HSQLDBStrategy();
//                    System.out.println("HSQLDBStrategy");
                }
                
                if (pageSource.matches("(?si).*("
                        // jdbc
                        + "ERROR: unterminated quoted identifier at or near"
                        + "|"
                        // php
                        + "Query failed: ERROR:  unterminated quoted string at or near"
                        + "|"
                        // php
                        + "function\\.pg"
                        + "|"
                        + "PostgreSQL"
                + ").*")) {
                    MediatorModel.model().sqlStrategy = new PostgreSQLStrategy();
//                    System.out.println("PostgreSQLStrategy");
                }
                
                /**
Warning: oci_parse() [function.oci-parse]: ORA-01756: quoted string not properly terminated in E:\Outils\EasyPHP-5.3.9\www\oracle_simulate_get.php on line 6

Warning: oci_execute() expects parameter 1 to be resource, boolean given in E:\Outils\EasyPHP-5.3.9\www\oracle_simulate_get.php on line 7

Warning: oci_fetch_array() expects parameter 1 to be resource, boolean given in E:\Outils\EasyPHP-5.3.9\www\oracle_simulate_get.php on line 10

jdbc
Error at line 1:
ORA-01740: missing double quote in identifier
select '"'"'
          ^
                 */
                if (Pattern.compile(".*function\\.oci.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new OracleStrategy();
//                    System.out.println("OracleStrategy");
                }
                
                /**
                 * Fatal error: Uncaught exception 'PDOException' with message 'SQLSTATE[42000]: [Microsoft][SQL Server Native Client 11.0][SQL Server]An object or column name is missing or empty. For SELECT INTO statements, verify each column has a name. For other statements, look for empty alias names. Aliases defined as "" or [] are not allowed. Change the alias to a valid name.'
                 * or
                 * Fatal error: Uncaught exception 'PDOException' with message 'SQLSTATE[42000]: [Microsoft][SQL Server Native Client 11.0][SQL Server]Unclosed quotation mark after the character string
                 * 
                 * jdbc
                 * Unclosed quotation mark after the character string '''. [SQL State=S0001, DB Errorcode=105] 
                 */
                if (Pattern.compile(".*SQL Server.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new SQLServerStrategy();
//                    System.out.println("SQLServerStrategy");
                }
                
                /**
                 * Warning: db2_execute() [function.db2-execute]: Statement Execute Failed in E:\Outils\EasyPHP-5.3.9\www\db2_simulate_get.php on line 13
exec errormsg: [IBM][CLI Driver][DB2/NT] SQL0010N La constante commen ant par """ ne comporte pas de d limiteur de fin de cha ne. SQLSTATE=42603
Warning: db2_fetch_array() [function.db2-fetch-array]: Column information cannot be retrieved in E:\Outils\EasyPHP-5.3.9\www\db2_simulate_get.php on line 17

                 * jdbc
                 * DB2 SQL Error: SQLCODE=-10, SQLSTATE=42603, SQLERRMC="', DRIVER=3.69.24 [SQL State=42603, DB Errorcode=-10] 
Next: DB2 SQL Error: SQLCODE=-727, SQLSTATE=56098, SQLERRMC=2;-10;42603;"', DRIVER=3.69.24 [SQL State=56098, DB Errorcode=-727] 
                 */
                if (Pattern.compile(".*function\\.db2.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new DB2Strategy();
//                    System.out.println("DB2Strategy");
                }
                
                /**
                 * Query failed: line 1, Non-terminated string
                 * 
                 * jdbc
                 * Unmatched quote, parenthesis, bracket or brace. [SQL State=42000, DB Errorcode=802835] 
                 */
                if (Pattern.compile(".*Non-terminated string.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new IngresStrategy();
//                    System.out.println("IngresStrategy");
                }
                
                /**
                 * Warning: sybase_connect() [function.sybase-connect]: Sybase: Server message: Changed database context to 'master'. (severity 10, procedure N/A) in E:\Dev\xampp-win32-1.7.4-VC6\htdocs\sybase\sybase_simulate_get.php on line 5

Warning: sybase_query() [function.sybase-query]: Sybase: Server message: Unclosed quote before the character string '\"'. (severity 15, procedure N/A) in E:\Dev\xampp-win32-1.7.4-VC6\htdocs\sybase\sybase_simulate_get.php on line 10

Warning: sybase_query() [function.sybase-query]: Sybase: Server message: Incorrect syntax near '\"'. (severity 15, procedure N/A) in E:\Dev\xampp-win32-1.7.4-VC6\htdocs\sybase\sybase_simulate_get.php on line 10

Warning: sybase_fetch_row() expects parameter 1 to be resource, boolean given in E:\Dev\xampp-win32-1.7.4-VC6\htdocs\sybase\sybase_simulate_get.php on line 14

jdbc
Invalid SQL statement or JDBC escape, terminating '"' not found. [SQL State=22025] 
                 */
                if (Pattern.compile(".*function\\.sybase.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new SybaseStrategy();
//                    System.out.println("SybaseStrategy");
                }
                
                /**
                 *  Warning: maxdb::query() [maxdb.query]: -3014 POS(40) Invalid end of SQL statement [42000] in E:\Dev\xampp-win32-1.6.8\htdocs\maxdb\maxdb_simulate_get.php on line 40
                 *  
                 * jdbc
                 * [-3014] (at 12): Invalid end of SQL statement

select '"'"'
           ^
                 */
                if (Pattern.compile(".*maxdb\\.query.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new MaxDbStrategy();
//                    System.out.println("MaxDbStrategy");
                }
                
                /**
                 * SQLSTATE[HY000]: General error: -11060 [Informix][Informix ODBC Driver]General error. (SQLPrepare[-11060] at ext\PDO_INFORMIX-1.3.1\informix_driver.c:131)
                 * 
                 * jdbc
                 * Found a quote for which there is no matching quote. [SQL State=IX000, DB Errorcode=-282] 
                 */
                if (Pattern.compile(".*Informix.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new InformixStrategy();
//                    System.out.println("InformixStrategy");
                }
                
                /**
                 * 
Warning: ibase_query() [function.ibase-query]: Dynamic SQL Error SQL error code = -104 as approximate floating-point values in SQL dialect 1, but as 64-bit in E:\Dev\xampp-win32-1.6.8\htdocs\firebird\firebird_simulate_get.php on line 27
Dynamic SQL Error SQL error code = -104 as approximate floating-point values in SQL dialect 1, but as 64-bit

jdbc
GDS Exception. 335544569. Dynamic SQL Error
SQL error code = -104
Unexpected end of command - line 1, column 11

select '"'"'
                 */
                if (Pattern.compile(".*function\\.ibase-query.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new FirebirdStrategy();
//                    System.out.println("FirebirdStrategy");
                }
                if (Pattern.compile(".*hsqldb.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new HSQLDBStrategy();
//                    System.out.println("HSQLDBStrategy");
                }
                if (Pattern.compile(".*derby.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new DerbyStrategy();
//                    System.out.println("DerbyStrategy");
                }
                if (Pattern.compile(".*cubrid.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new CubridStrategy();
//                    System.out.println("CubridStrategy");
                }
                if (Pattern.compile(".*teradata.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new TeradataStrategy();
//                    System.out.println("TeradataStrategy");
                }
                if (Pattern.compile(".*h2.*", Pattern.DOTALL).matcher(pageSource).matches()) {
                    MediatorModel.model().sqlStrategy = new H2Strategy();
//                    System.out.println("H2Strategy");
                }
            } catch (InterruptedException e) {
                LOGGER.error(e, e);
            } catch (ExecutionException e) {
                LOGGER.error(e, e);
            }
        }
        return null;
    }
}