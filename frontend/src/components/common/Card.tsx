import React from 'react';

interface CardProps {
  children: React.ReactNode;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
  bodyClassName?: string;
}

export const Card: React.FC<CardProps> = ({
  children,
  title,
  subtitle,
  action,
  footer,
  className = '',
  bodyClassName = 'p-6',
}) => {
  return (
    <div className={`bg-white border border-slate-200/90 rounded-xl shadow-xs overflow-hidden ${className}`}>
      {(title || subtitle || action) && (
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between gap-4">
          <div>
            {title && <h3 className="text-base font-semibold text-slate-900 leading-tight">{title}</h3>}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}
      <div className={bodyClassName}>{children}</div>
      {footer && <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100">{footer}</div>}
    </div>
  );
};
