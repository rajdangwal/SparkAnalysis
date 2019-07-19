import soot.*;

import java.util.HashMap;
import java.util.Map;

import soot.toolkits.graph.*;
import soot.jimple.spark.*;

public class SparkExample
{

	public static void main(String args[])
	{
		if(args.length==0)
		{
			System.out.println("Usage: java SparkExample class_to_analyze");
			System.exit(0);
		}
		else
		{
			System.out.println("Analyzing class: "+args[0]);
		}

		 String classPath, mainClass;

		classPath = "classes:/usr/local/java/jdk1.7.0_80/jre/lib/rt.jar:/usr/local/java/jdk1.7.0_80/jre/lib/jce.jar";
		mainClass = args[0];

		String sootArgs[] = {"-cp", classPath, "-pp", "-main-class", mainClass, "-ws", "-process-dir", "classes/", "-p", "wsop", "enabled:true", "-p", "cg", "enabled:true", "-p", "cg.spark", "enabled:true", args[0]};

		// loadClass("Item", false);
		// loadClass("Container", false);
		// SootClass c = loadClass(args[0], true);


		PackManager.v().getPack("wsop").add(new Transform("wsop.myspark", new SceneTransformer(){
			protected void internalTransform(String phaseName, Map options)
			{
				Map options2 = new HashMap();
				options2.put("verbose", "true");
				options2.put("propagator", "worklist");
				options2.put("simple-edges-bidirectional", "false");
				options2.put("on-fly-cg", "true");
				options2.put("set-impl", "hybrid");
				options2.put("double-set-old", "hybrid");
				options2.put("double-set-new", "hybrid");
				SparkTransformer.v().transform("",options2);
			}
		}));
		//PackManager.v().getPack("wjtp").add(new Transform("wjtp.cggenerator", analysisTransformer));

		soot.Main.main(sootArgs);


	}
}