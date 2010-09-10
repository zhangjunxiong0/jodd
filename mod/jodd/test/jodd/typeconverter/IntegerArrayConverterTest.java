// Copyright (c) 2003-2010, Jodd Team (jodd.org). All Rights Reserved.

package jodd.typeconverter;

public class IntegerArrayConverterTest extends BaseTestCase {

	public void testConversion() {
		assertNull(IntegerArrayConverter.valueOf(null));

		assertEq(arri(173), IntegerArrayConverter.valueOf(Double.valueOf(173)));
		assertEq(arri(1, 7, 3), IntegerArrayConverter.valueOf(arri(1, 7, 3)));
		assertEq(arri(173, 1022), IntegerArrayConverter.valueOf(arrs("173", "1022")));
		assertEq(arri(173, 10), IntegerArrayConverter.valueOf(arr("173", Integer.valueOf(10))));
	}

	private void assertEq(int[] arr1, int[] arr2) {
		assertEquals(arr1.length, arr2.length);
		for (int i = 0; i < arr1.length; i++) {
			assertEquals(arr1[i], arr2[i]);
		}
	}

}

