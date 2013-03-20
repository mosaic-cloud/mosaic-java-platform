
package eu.mosaic_cloud.platform.core.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import eu.mosaic_cloud.platform.core.utils.DataEncoder.EncodeOutcome;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.junit.Test;


public class JsonDataEncoderTest
{
	@Test
	public void testArrayContainer ()
			throws Throwable
	{
		final ArrayContainer input = new ArrayContainer ();
		final ArrayContainer output = this.testEncodeDecode (input);
	}
	
	@Test
	public void testArrayListContainer ()
			throws Throwable
	{
		final ArrayListContainer input = new ArrayListContainer ();
		final ArrayListContainer output = this.testEncodeDecode (input);
	}
	
	@Test
	public void testIssue01VariantA ()
			throws Throwable
	{
		final Issue01VariantA input = new Issue01VariantA ();
		final Issue01VariantA output = this.testEncodeDecode (input);
	}
	
	@Test
	public void testIssue01VariantB ()
			throws Throwable
	{
		final Issue01VariantB input = new Issue01VariantB ();
		final Issue01VariantB output = this.testEncodeDecode (input);
	}
	
	@Test
	public void testSimpleObject ()
			throws Throwable
	{
		final SimpleObject input = new SimpleObject ();
		final SimpleObject output = this.testEncodeDecode (input);
	}
	
	protected <TObject> TObject testEncodeDecode (final TObject input)
			throws EncodingException
	{
		final JsonDataEncoder<TObject> encoder = JsonDataEncoder.create ((Class<TObject>) input.getClass (), false);
		final EncodeOutcome outcome = encoder.encode (input, null);
		final TObject output = encoder.decode (outcome.data, outcome.metadata);
		return (output);
	}
	
	public static class ArrayContainer
	{
		public int[] intArray = new int[] {0, 1, 2, 3};
		public int[][] intMatrix = new int[][] { {1, 2}, {3, 4}};
		public String[] stringArray = new String[] {"a", "b", null, "d"};
		public String[] stringArrayIsNull = null;
	}
	
	public static class ArrayListContainer
	{
		public ArrayList<String> arrayList = new ArrayList<String> (Arrays.asList ("a", "b", "c"));
	}
	
	public static class Issue01VariantA
			implements
				Serializable
	{
		public Issue01VariantA ()
		{
			this.list = new String[6];
		}
		
		public int cont = 0;
		public int i = 0;
		public String list[] = null;
		public String prova = null;
		private static final long serialVersionUID = 1L;
	}
	
	public static class Issue01VariantB
			implements
				Serializable
	{
		public Issue01VariantB ()
		{
			this.list = new String[6];
		}
		
		public void addCSP (final String cspname)
		{
			this.list[this.i] = cspname;
			this.i++;
		}
		
		public void addString (final String val)
		{
			this.prova = val;
		}
		
		public String get (final int indice)
		{
			return this.list[indice];
		}
		
		@JsonIgnore
		public int getSize ()
		{
			this.cont = this.i;
			return this.cont + 1;
		}
		
		public int cont = 0;
		public int i = 0;
		public String list[] = null;
		public String prova = null;
		private static final long serialVersionUID = 1L;
	}
	
	public static class SimpleObject
	{
		public boolean booleanField = false;
		public double doubleField = 99.99e-9;
		public int intField = 99;
		public String stringField = "string";
		public String stringFieldIsNull = null;
	}
}
