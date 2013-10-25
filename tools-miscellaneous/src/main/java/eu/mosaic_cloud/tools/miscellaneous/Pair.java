
package eu.mosaic_cloud.tools.miscellaneous;


import com.google.common.base.Objects;


public final class Pair<_First_ extends Object, _Second_ extends Object>
			extends Object
{
	protected Pair (final _First_ first, final _Second_ second) {
		super ();
		this.first = first;
		this.second = second;
	}
	
	@Override
	public final Pair<_First_, _Second_> clone () {
		return (Pair.create (this.first, this.second));
	}
	
	@Override
	public final boolean equals (final Object other) {
		if (this == other)
			return (true);
		if (other == null)
			return (false);
		if (!(other instanceof Pair))
			return (false);
		final Pair<?, ?> pair = (Pair<?, ?>) other;
		return (Objects.equal (this.first, pair.first) && Objects.equal (this.second, pair.second));
	}
	
	@Override
	public final int hashCode () {
		return (Objects.hashCode (this.first, this.second));
	}
	
	@Override
	public final String toString () {
		return (Objects.toStringHelper ("Pair").add ("first", this.first).add ("second", this.second).toString ());
	}
	
	public final <_First_ extends Object> Pair<_First_, _Second_> updateFirst (final _First_ first) {
		return (Pair.create (first, this.second));
	}
	
	public final <_Second_ extends Object> Pair<_First_, _Second_> updateSecond (final _Second_ second) {
		return (Pair.create (this.first, second));
	}
	
	public final _First_ first;
	public final _Second_ second;
	
	public static final <_First_ extends Object, _Second_ extends Object> Pair<_First_, _Second_> create (final _First_ first, final _Second_ second) {
		return (new Pair<_First_, _Second_> (first, second));
	}
}
