import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Java 17 source launcher: manage only the existing workspace MySQL, never initialize data. */
class StartLocalDatabase {
    public static void main(String[] args) throws Exception {
        Path root=Path.of("").toAbsolutePath().normalize();
        Path runtime=root.resolve(".runtime"), server=runtime.resolve("mysql-8.4.0-winx64/bin/mysqld.exe");
        Path admin=server.resolveSibling("mysqladmin.exe"), config=runtime.resolve("mysql-admin.local.ini");
        if(!Files.isRegularFile(server)||!Files.isRegularFile(admin)||!Files.isRegularFile(config)
                ||!Files.isDirectory(runtime.resolve("mysql-data")))
            throw new IllegalStateException("Existing local MySQL installation/configuration missing; see docs/DATABASE.md");
        if(ready(admin,config)){System.out.println("Workspace MySQL is ready on 127.0.0.1:3307.");return;}
        boolean running=ProcessHandle.allProcesses().anyMatch(p->p.info().command()
                .map(c->Path.of(c).toAbsolutePath().normalize().equals(server)).orElse(false));
        Process started=null;
        if(!running){
            started=new ProcessBuilder(server.toString(),"--no-defaults",
                    "--basedir="+runtime.resolve("mysql-8.4.0-winx64"),
                    "--datadir="+runtime.resolve("mysql-data"),"--port=3307","--bind-address=127.0.0.1",
                    "--mysqlx=OFF","--log-error="+runtime.resolve("mysql-server.log"))
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(runtime.resolve("mysql-start.log").toFile())).start();
        }
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
        do {
            if(ready(admin,config)){System.out.println("Workspace MySQL is ready on 127.0.0.1:3307.");return;}
            if(started!=null&&!started.isAlive())throw new IllegalStateException("Local MySQL exited; see .runtime/mysql-server.log");
        } while(System.nanoTime()<deadline);
        throw new IllegalStateException("Local MySQL not ready within 30 seconds; see .runtime/mysql-server.log");
    }
    private static boolean ready(Path admin,Path config)throws Exception {
        Process ping=new ProcessBuilder(admin.toString(),"--defaults-extra-file="+config,
                "--host=127.0.0.1","--port=3307","--connect-timeout=1","ping","--silent")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if(!ping.waitFor(2,TimeUnit.SECONDS)){ping.destroyForcibly();return false;}
        return ping.exitValue()==0;
    }
}
