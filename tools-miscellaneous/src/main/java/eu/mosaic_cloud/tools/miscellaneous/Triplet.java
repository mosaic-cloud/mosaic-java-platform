
package eu.mosaic_cloud.tools.miscellaneous;


import com.google.common.base.Objects;


public final class Triplet<_First_ extends Object, _Second_ extends Object, _Third_ extends Object>
			extends Object
{
	protected Triplet (final _First_ first, final _Second_ second, final _Third_ third) {
		super ();
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	@Override
	public final Triplet<_First_, _Second_, _Third_> clone () {
		return (Triplet.create (this.first, this.second, this.third));
	}
	
	@Override
	public final boolean equals (final Object other) {
		if (this == other)
			return (true);
		if (other == null)
			return (false);
		if (!(other instanceof Triplet))
			return (false);
		final Triplet<?, ?, ?> triplet = (Triplet<?, ?, ?>) other;
		return (Objects.equal (this.first, triplet.first) && Objects.equal (this.second, triplet.second) && Objects.equal (this.third, triplet.third));
	}
	
	@Override
	public final int hashCode () {
		return (Objects.hashCode (this.first, this.second, this.third));
	}
	
	@Override
	public final String toString () {
		return (Objects.toStringHelper ("Pair").add ("first", this.first).add ("second", this.second).add ("third", this.third).toString ());
	}
	
	public final <_First_ extends Object> Triplet<_First_, _Second_, _Third_> updateFirst (final _First_ first) {
		return (Triplet.create (first, this.second, this.third));
	}
	
	public final <_Second_ extends Object> Triplet<_First_, _Second_, _Third_> updateSecond (final _Second_ second) {
		return (Triplet.create (this.first, second, this.third));
	}
	
	public final <_Third_ extends Object> Triplet<_First_, _Second_, _Third_> updateThird (final _Third_ third) {
		return (Triplet.create (this.first, this.second, third));
	}
	
	public final _First_ first;
	public final _Second_ second;
	public final _Third_ third;
	
	public static final <_First_ extends Object, _Second_ extends Object, _Third_ extends Object> Triplet<_First_, _Second_, _Third_> create (final _First_ first, final _Second_ second, final _Third_ third) {
		return (new Triplet<_First_, _Second_, _Third_> (first, second, third));
	}
}
