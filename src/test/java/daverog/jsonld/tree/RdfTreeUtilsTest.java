package daverog.jsonld.tree;

import java.util.Comparator;

import org.junit.Test;

import com.google.common.collect.Lists;

import daverog.jsonld.tree.RdfTreeUtils;

import static org.junit.Assert.assertEquals;

public class RdfTreeUtilsTest {
	
	private static Comparator<String> stringComparator = new Comparator<String>(){
			public int compare(String one, String two) {
				return one.compareTo(two);
			}
		};

	@Test
	public void ifTwoListsAreEmptyTheyAreEqual() {
		assertEquals(0, RdfTreeUtils.compareTwoListsOfValues(
				Lists.<String>newArrayList(), 
				Lists.<String>newArrayList(),
				stringComparator));
	}
	
	@Test
	public void ifFirstListIsOnlyOneWithValuesItIsLessThanTheSecond() {
		assertEquals(-1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.newArrayList("a"), 
				Lists.<String>newArrayList(),
				stringComparator));
	}
	
	@Test
	public void ifSecondListIsOnlyOneWithValuesItIsLessThanTheFirst() {
		assertEquals(1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.<String>newArrayList(),
				Lists.newArrayList("a"),
				stringComparator));
	}

	
	@Test
	public void ifFirstListContainsAValueThatIsLessThanTheSecondItIsLessThanTheSecond() {
		assertEquals(-1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.newArrayList("a"), 
				Lists.newArrayList("b"),
				stringComparator));
	}
	
	@Test
	public void ifFirstListContainsAValueThatIsMoreThanTheSecondItIsMoreThanTheSecond() {
		assertEquals(1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.newArrayList("b"), 
				Lists.newArrayList("a"),
				stringComparator));
	}
	
	@Test
	public void ifFirstListContainsAValueThatIsMoreThanOnlySomeOfTheSecondItIsMoreThanTheSecond() {
		assertEquals(1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.newArrayList("b"), 
				Lists.newArrayList("a", "c"),
				stringComparator));
	}
	
	@Test
	public void ifFirstListContainsValuesThatAreBothMoreAndLessThanTheSecondItIsLessThanTheSecond() {
		assertEquals(-1, RdfTreeUtils.compareTwoListsOfValues(
				Lists.newArrayList("a", "c"), 
				Lists.newArrayList("b"),
				stringComparator));
	}

	
}
