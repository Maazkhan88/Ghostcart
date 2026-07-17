type BrandProps = {
  compact?: boolean;
  light?: boolean;
};

export function Brand({ compact = false, light = false }: BrandProps) {
  return (
    <span className={`gc-brand${compact ? " is-compact" : ""}${light ? " is-light" : ""}`}>
      <img src="/brand/ghost-cart-icon.png" alt="" width="34" height="34" />
      <span aria-label="Ghost Cart">
        <strong>Ghost</strong>
        <strong>Cart</strong>
      </span>
    </span>
  );
}
