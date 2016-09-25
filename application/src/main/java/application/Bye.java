package application;

import framework.api.HeaderParam;
import framework.api.Path;
import framework.api.QueryParam;

public class Bye {
    @Path("/bye")
    public String sayBye() {
        return "Bye!";
    }
    @Path("/say")
    public String say(@QueryParam("e") Keys e) {
        return "Yes,it's good "+e+" is "+e.ordinal();
    }
    @Path("/xor")
    public Boolean say(@QueryParam("a") Boolean a,@QueryParam("b") Boolean b) {
        return a^b;
    }
    @Path("/intSum")
    public int sum(@QueryParam("a") Integer a,@QueryParam("b") Integer b) {
        return a+b;
    }
    @Path("/headerSum")
    public int headerSum(@HeaderParam("a") Integer a, @HeaderParam("b") Integer b) {
        return a+b;
    }
}
