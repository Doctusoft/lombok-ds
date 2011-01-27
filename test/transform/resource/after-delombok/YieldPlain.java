import static lombok.Yield.yield;

import java.util.Iterator;
import java.util.Iterable;

import javax.swing.JScrollPane;
class YieldPlain {
	public Iterator<Long> fib_while() {
		class $YielderFibWhile implements Iterator<Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private Long $next;

			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}

			public Long next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}

			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while(true) {
					switch ($state) {
					case 0:
						a = 0;
						b = 1;
					case 1:
						$next = a;
						$state = 2;
						return true;
					case 2:
						c = a + b;
						if (c < 0) {
							$state = 3;
							continue;
						}
						a = b;
						b = c;
						$state = 1;
						continue;
					default:
					case 3:
					}
					return false;
				}
			}
		}
		return new $YielderFibWhile();
	}
	
	public Iterator<Long> fib_while_2() {
		class $YielderFibWhile2 implements Iterator<Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private Long $next;

			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}

			public Long next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}

			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while(true) {
					switch ($state) {
					case 0:
						a = 0;
						b = 1;
					case 1:
						if (b < 0) {
							$state = 3;
							continue;
						}
						$next = a;
						$state = 2;
						return true;
					case 2:
						c = a + b;
						a = b;
						b = c;
						$state = 1;
						continue;
					default:
					case 3:
					}
					return false;
				}
			}
		}
		return new $YielderFibWhile2();
	}

	public Iterable<Long> fib_for() {
		class $YielderFibFor implements Iterator<Long>, Iterable<Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private Long $next;

			@Override
			public Iterator<Long> iterator() {
				return new $YielderFibFor();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}

			public Long next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}

			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while(true) {
					switch ($state) {
					case 0:
						a = 0;
						b = 1;
					case 1:
						if (b < 0) {
							$state = 3;
							continue;
						}
						$next = a;
						$state = 2;
						return true;
					case 2:
						c = a + b;
						a = b;
						b = c;
						$state = 1;
						continue;
					default:
					case 3:
					}
					return false;
				}
			}
		}
		return new $YielderFibFor();
	}
	
	public Iterable<String> complex_foreach(final Iterable<Object> objects) {
		class $YielderComplexForeach implements Iterator<String>, Iterable<String> {
			private Iterator<Object> $objectIter;
			private Object object;
			private Class<?> c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private String $next;

			@Override
			public Iterator<String> iterator() {
				return new $YielderComplexForeach();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}

			public String next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}

			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while(true) {
					switch ($state) {
					case 0:
						$objectIter = objects.iterator();
					case 1:
						if (!$objectIter.hasNext()) {
							$state = 5;
							continue;
						}
						object = $objectIter.next();
						
						if (object instanceof Class<?>) {
							c = (Class<?>) object;
							$next = "A String";
							$state = 2;
							return true;
						}
						$state = 4;
						continue;
					case 2:
						$next = c.getName();
						$state = 3;
						return true;
					case 3:
						$state = 5;
						continue;
					case 4:
						$next = object.toString();
						$state = 1;
						return true;
					case 5:
						$next = "Another String";
						$state = 6;
						return true;
					default:
					case 6:
					}
					return false;
				}
			}
		}
		return new $YielderComplexForeach();
	}
	
	public Iterator<String> complex(final Iterator<Object> objects) {
		class $YielderComplex implements Iterator<String> {
			private Object object;
			private Class<?> c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private String $next;
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}

			public String next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}

			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while(true) {
					switch ($state) {
					case 0:
						$next = "Another String";
						$state = 1;
						return true;
					case 1:
						if (!objects.hasNext()) {
							$state = 5;
							continue;
						}
						object = objects.next();
						
						if (object instanceof Class<?>) {
							c = (Class<?>) object;
							$next = "A String";
							$state = 2;
							return true;
						}
						$state = 4;
						continue;
					case 2:
						$next = c.getName();
						$state = 3;
						return true;
					case 3:
						$state = 5;
						continue;
					case 4:
						$next = object.toString();
						$state = 1;
						return true;
					case 5:
						$next = "Another String";
						$state = 6;
						return true;
					default:
					case 6:
					}
					return false;
				}
			}
		}
		return new $YielderComplex();
	}
}
