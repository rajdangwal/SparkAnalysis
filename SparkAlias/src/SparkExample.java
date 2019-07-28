import soot.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import soot.util.Chain;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

public class SparkExample
{
	static long undecided = 0;
	static long selected = 0;
	static long nonSelected = 0;

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

		classPath = "/home/raju/temp/RoadRunner-0.5/rapid_benchmarks/small/:/usr/local/java/jdk1.7.0_80/jre/lib/rt.jar:/usr/local/java/jdk1.7.0_80/jre/lib/jce.jar";
		mainClass = args[0];

		String sootArgs[] = {"-cp", classPath, "-pp", "-coffi", "-keep-bytecode-offset", "-main-class", mainClass, "-ws", "-process-dir", "/home/raju/temp/RoadRunner-0.5/rapid_benchmarks/small/", "-f", "S", "-p", "wsop", "enabled:true", "-p", "cg", "enabled:true", "-p", "cg.spark", "enabled:true", "-p", "jb", "use-original-names", args[0]};


		PackManager.v().getPack("wsop").add(new Transform("wsop.myspark", new SceneTransformer(){
			protected void internalTransform(String phaseName, Map options)
			{
				PointsToAnalysis pta =  Scene.v().getPointsToAnalysis();
				
				Chain<SootClass> classChain = Scene.v().getApplicationClasses();
				
				BriefBlockGraph basicBlockGraph;
				List<Block> basicBlockList;
				
				for(SootClass s: classChain)
				{
					System.out.println("\nClass: "+ s + "\n");
					List<SootMethod> methodList = s.getMethods();
					
					for(SootMethod m: methodList)
					{
						Map<Local, DoublePointsToSet> pointsToMap = new LinkedHashMap<Local, DoublePointsToSet>();
						Map<String, LinkedHashSet<Local>> reversePointsToMap = new LinkedHashMap<String, LinkedHashSet<Local>>();
						Set<Local> localsToInstrument = new HashSet<Local>();
						Set<Local> localsNotToInstrument = new HashSet<Local>();
						Set<Local> localsUnDecided = new HashSet<Local>();
						
						
						System.out.println("\nMethod: "+ m + "\n");
						
						basicBlockGraph = new BriefBlockGraph(m.retrieveActiveBody());
						basicBlockList = new LinkedList<Block>(basicBlockGraph.getBlocks());
						
						for (Block bB : basicBlockList) {
							Iterator<Unit> blockIterator = bB.iterator();
							while (blockIterator.hasNext()) {
								Unit u = (Unit) blockIterator.next();
								Stmt st = (Stmt) u;
								
								if (st instanceof JAssignStmt) {
									Value vL = ((JAssignStmt) st).getLeftOp();
									Value vR = ((JAssignStmt) st).getRightOp();
									Type tR = vR.getType();
									
									if (vL instanceof JimpleLocal) {
										if (tR instanceof RefType && !(vR instanceof PhiExpr)) {
											if (!(vR instanceof JNewExpr)) {
												if (!((RefType) tR).getSootClass().isJavaLibraryClass()) {
													Local l = (Local)vL;
													if(!pointsToMap.containsKey(l)) {//is this check necessary in SSA
														PointsToSet pts = pta.reachingObjects(l);
														DoublePointsToSet dpts;
														if(!(pts instanceof EmptyPointsToSet))
														{
															dpts = (DoublePointsToSet) pts;
															pointsToMap.put(l, dpts);
														}
													}
												}
											}
										}
										else if(tR instanceof ArrayType && !(vR instanceof PhiExpr)) {
											Local l = (Local)vL;
											if(!pointsToMap.containsKey(l)) {
												PointsToSet pts = pta.reachingObjects(l);
												DoublePointsToSet dpts;
												if(!(pts instanceof EmptyPointsToSet))
												{
													dpts = (DoublePointsToSet) pts;
													pointsToMap.put(l, dpts);
												}
											}
										}
										
									}
								
								}
							}
						}
						
						//printing Map of Local to PointsToSet (Locals initially chosen for instrumentation)
						for(Map.Entry<Local, DoublePointsToSet> e : pointsToMap.entrySet() )
						{
							System.out.println("Local: "+e.getKey() + "\nPoints to: "+ e.getValue());
						}
						
						
						
						for(Map.Entry<Local, DoublePointsToSet> e : pointsToMap.entrySet())
						{
							DoublePointsToSet ptsi = e.getValue();
							int size = ptsi.size();
							if(size == 1)
							{
								String node = ptsi.toString();
								Local l = e.getKey();
								if(!(reversePointsToMap.containsKey(node)))
								{
									LinkedHashSet<Local> localSet = new LinkedHashSet<Local>();
									localSet.add(l);
									reversePointsToMap.put(node, localSet);
									localsToInstrument.add(l);
								}
								else
								{
									reversePointsToMap.get(node).add(l);
									localsNotToInstrument.add(l);
								}
							}
							else
							{
								localsUnDecided.add(e.getKey());
							}
						}
						
						//Printing Map of String to Local's Set (Locals are must aliases)
						for(Map.Entry<String, LinkedHashSet<Local>> e : reversePointsToMap.entrySet() )
						{
							System.out.println("Object: "+e.getKey() + "   Locals: "+ e.getValue());
						}
						
						
						
						
						
						System.out.println("LocalsToInstrument size: "+localsToInstrument.size());
						selected += localsToInstrument.size();
						for(Local l : localsToInstrument)
						{
							System.out.print(l + " ");
						}
						
						System.out.println("\nLocalsNotToInstrument size: "+localsNotToInstrument.size());
						nonSelected += localsNotToInstrument.size();
						for(Local l : localsNotToInstrument)
						{
							System.out.print(l + " ");
						}
						
						System.out.println("\nLocalsUndecided size: "+localsUnDecided.size());
						undecided += localsUnDecided.size();
						for(Local l : localsUnDecided)
						{
							System.out.print(l + " ");
						}
						
						System.out.println();
						
					}
				}
			}
		}));

		soot.Main.main(sootArgs);
		System.out.println("Selected: "+ selected);
		System.out.println("Non selected: "+ nonSelected);
		System.out.println("UnDecided: "+ undecided);

	}
}