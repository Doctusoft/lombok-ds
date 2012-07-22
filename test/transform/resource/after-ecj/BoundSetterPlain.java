import lombok.AccessLevel;
import lombok.BoundSetter;
class BoundSetterPlain {
  private volatile transient java.beans.PropertyChangeSupport $propertyChangeSupport;
  private final java.lang.Object[] $propertyChangeSupportLock = new java.lang.Object[0];
  public static final java.lang.String PROP_I = "i";
  public static final java.lang.String PROP_S = "s";
  public static final java.lang.String PROP_F = "f";
  public static final java.lang.String PROP_O = "o";
  public static final java.lang.String PROP_D = "d";
  @BoundSetter int i;
  @BoundSetter(AccessLevel.PUBLIC) String s;
  @BoundSetter(AccessLevel.PROTECTED) float f;
  @BoundSetter(AccessLevel.PACKAGE) Object o;
  @BoundSetter(AccessLevel.PRIVATE) double d;
  <clinit>() {
  }
  BoundSetterPlain() {
    super();
  }
  private @java.lang.SuppressWarnings("all") java.beans.PropertyChangeSupport getPropertyChangeSupport() {
    if ((this.$propertyChangeSupport == null))
        {
          synchronized (this.$propertyChangeSupportLock)
            {
              if ((this.$propertyChangeSupport == null))
                  {
                    this.$propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
                  }
            }
        }
    return this.$propertyChangeSupport;
  }
  public @java.lang.SuppressWarnings("all") void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    getPropertyChangeSupport().addPropertyChangeListener(listener);
  }
  public @java.lang.SuppressWarnings("all") void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    getPropertyChangeSupport().removePropertyChangeListener(listener);
  }
  public @java.lang.SuppressWarnings("all") void firePropertyChange(final java.lang.String propertyName, final java.lang.Object oldValue, final java.lang.Object newValue) {
    getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
  }
  public @java.lang.SuppressWarnings("all") void setI(final int i) {
    final int $old = this.i;
    this.i = i;
    firePropertyChange(PROP_I, $old, i);
  }
  public @java.lang.SuppressWarnings("all") void setS(final String s) {
    final String $old = this.s;
    this.s = s;
    firePropertyChange(PROP_S, $old, s);
  }
  protected @java.lang.SuppressWarnings("all") void setF(final float f) {
    final float $old = this.f;
    this.f = f;
    firePropertyChange(PROP_F, $old, f);
  }
  @java.lang.SuppressWarnings("all") void setO(final Object o) {
    final Object $old = this.o;
    this.o = o;
    firePropertyChange(PROP_O, $old, o);
  }
  private @java.lang.SuppressWarnings("all") void setD(final double d) {
    final double $old = this.d;
    this.d = d;
    firePropertyChange(PROP_D, $old, d);
  }
}