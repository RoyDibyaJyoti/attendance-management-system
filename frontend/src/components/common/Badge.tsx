import React from 'react';

export type BadgeVariant = 'success' | 'danger' | 'warning' | 'info' | 'neutral' | 'purple';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
  className?: string;
  dot?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
  dot = false,
}) => {
  const variantStyles: Record<BadgeVariant, { container: string; dot: string }> = {
    success: {
      container: 'bg-emerald-50 text-emerald-700 border-emerald-200/80',
      dot: 'bg-emerald-500',
    },
    danger: {
      container: 'bg-rose-50 text-rose-700 border-rose-200/80',
      dot: 'bg-rose-500',
    },
    warning: {
      container: 'bg-amber-50 text-amber-800 border-amber-200/80',
      dot: 'bg-amber-500',
    },
    info: {
      container: 'bg-blue-50 text-blue-700 border-blue-200/80',
      dot: 'bg-blue-500',
    },
    purple: {
      container: 'bg-indigo-50 text-indigo-700 border-indigo-200/80',
      dot: 'bg-indigo-500',
    },
    neutral: {
      container: 'bg-slate-100 text-slate-700 border-slate-200',
      dot: 'bg-slate-400',
    },
  };

  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-xs font-semibold px-2.5 py-1',
  };

  const currentVariant = variantStyles[variant];

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border ${sizeClasses[size]} ${currentVariant.container} ${className}`}
    >
      {dot && <span className={`w-1.5 h-1.5 rounded-full ${currentVariant.dot}`} />}
      <span>{children}</span>
    </span>
  );
};
