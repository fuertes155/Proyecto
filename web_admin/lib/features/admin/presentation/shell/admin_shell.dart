import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/ui.dart';
import '../providers/admin_provider.dart';
import 'admin_nav.dart';

/// Marco del panel: menú lateral + contenido.
///
/// ≥ 1100 px: menú completo · 860–1100 px: solo íconos · < 860 px: menú desplegable.
class AdminShell extends StatelessWidget {
  const AdminShell({super.key, required this.location, required this.child});

  final String location;
  final Widget child;

  static const double _wide = 1100;
  static const double _medium = 860;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, constraints) {
      final width = constraints.maxWidth;
      if (width >= _medium) {
        return Scaffold(
          body: Row(
            children: [
              _Sidebar(location: location, collapsed: width < _wide),
              Expanded(child: child),
            ],
          ),
        );
      }
      return Scaffold(
        appBar: AppBar(
          backgroundColor: MetColors.surface,
          surfaceTintColor: Colors.transparent,
          shape: const Border(bottom: BorderSide(color: MetColors.border)),
          titleSpacing: 0,
          leading: Builder(
            builder: (context) => IconButton(
              tooltip: 'Abrir menú',
              icon: const Icon(Icons.menu_rounded, color: MetColors.text),
              onPressed: () => Scaffold.of(context).openDrawer(),
            ),
          ),
          title: const Row(
            children: [
              MetLogo(size: 30),
              SizedBox(width: 10),
              Text('MET Admin', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18)),
            ],
          ),
        ),
        drawer: Drawer(
          width: 290,
          shape: const RoundedRectangleBorder(),
          child: _Sidebar(location: location, collapsed: false, inDrawer: true),
        ),
        body: child,
      );
    });
  }
}

class _Sidebar extends ConsumerWidget {
  const _Sidebar({required this.location, required this.collapsed, this.inDrawer = false});

  final String location;
  final bool collapsed;
  final bool inDrawer;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final admin = ref.watch(adminAuthProvider).valueOrNull;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      width: collapsed ? 84 : (inDrawer ? 290 : 272),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [MetColors.brandDark, MetColors.brandMid],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Brand(collapsed: collapsed),
            Divider(color: Colors.white.withValues(alpha: 0.08), height: 1),
            Expanded(
              child: ListView(
                padding: EdgeInsets.symmetric(horizontal: collapsed ? 14 : 14, vertical: 12),
                children: [
                  for (final section in adminNavSections) ...[
                    if (collapsed)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 10),
                        child: Divider(color: Colors.white.withValues(alpha: 0.1), height: 1),
                      )
                    else
                      Padding(
                        padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
                        child: Text(
                          section.title.toUpperCase(),
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.45),
                            fontSize: 11.5,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1.1,
                          ),
                        ),
                      ),
                    for (final item in section.items)
                      _NavTile(
                        item: item,
                        selected: location == item.route || location.startsWith('${item.route}/'),
                        collapsed: collapsed,
                        onTap: () {
                          if (inDrawer) Navigator.of(context).pop();
                          context.go(item.route);
                        },
                      ),
                  ],
                ],
              ),
            ),
            Divider(color: Colors.white.withValues(alpha: 0.08), height: 1),
            _AdminFooter(
              name: admin?.fullName ?? 'Administrador',
              role: admin?.role ?? 'ADMIN',
              collapsed: collapsed,
              onLogout: () async {
                await ref.read(adminAuthProvider.notifier).logout();
                if (context.mounted) context.go('/admin/login');
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _Brand extends StatelessWidget {
  const _Brand({required this.collapsed});

  final bool collapsed;

  @override
  Widget build(BuildContext context) {
    if (collapsed) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(child: Tooltip(message: 'MET Admin · $kMetSlogan', child: MetLogo(size: 40))),
      );
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              MetLogo(size: 40),
              SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('MET Admin',
                        style: TextStyle(
                            color: Colors.white, fontSize: 18, fontWeight: FontWeight.w800, letterSpacing: -0.2)),
                    Text('Panel administrativo',
                        style: TextStyle(color: Color(0xB3FFFFFF), fontSize: 12.5)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.07),
              borderRadius: BorderRadius.circular(10),
              border: Border(left: BorderSide(color: MetColors.accent.withValues(alpha: 0.9), width: 3)),
            ),
            child: Text(
              kMetSlogan,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.85),
                fontSize: 12.5,
                height: 1.4,
                fontStyle: FontStyle.italic,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _NavTile extends StatefulWidget {
  const _NavTile({required this.item, required this.selected, required this.collapsed, required this.onTap});

  final AdminNavItem item;
  final bool selected;
  final bool collapsed;
  final VoidCallback onTap;

  @override
  State<_NavTile> createState() => _NavTileState();
}

class _NavTileState extends State<_NavTile> {
  bool _hover = false;

  @override
  void initState() {
    super.initState();
    if (widget.selected) _revealSoon();
  }

  @override
  void didUpdateWidget(covariant _NavTile oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.selected && !oldWidget.selected) _revealSoon();
  }

  /// Lleva la sección activa a la vista si el menú tiene scroll.
  void _revealSoon() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        Scrollable.ensureVisible(context, alignment: 0.5, duration: const Duration(milliseconds: 200));
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final selected = widget.selected;
    final bg = selected
        ? Colors.white.withValues(alpha: 0.13)
        : (_hover ? Colors.white.withValues(alpha: 0.06) : Colors.transparent);
    final fg = selected ? Colors.white : Colors.white.withValues(alpha: 0.74);

    Widget tile = MouseRegion(
      cursor: SystemMouseCursors.click,
      onEnter: (_) => setState(() => _hover = true),
      onExit: (_) => setState(() => _hover = false),
      child: GestureDetector(
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 140),
          height: 40,
          margin: const EdgeInsets.only(bottom: 2),
          padding: EdgeInsets.symmetric(horizontal: widget.collapsed ? 0 : 12),
          decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(10)),
          child: Row(
            mainAxisAlignment: widget.collapsed ? MainAxisAlignment.center : MainAxisAlignment.start,
            children: [
              if (!widget.collapsed)
                AnimatedContainer(
                  duration: const Duration(milliseconds: 140),
                  width: 3,
                  height: 18,
                  margin: const EdgeInsets.only(right: 10),
                  decoration: BoxDecoration(
                    color: selected ? MetColors.accent : Colors.transparent,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              Icon(widget.item.icon, size: 20, color: selected ? Colors.white : fg),
              if (!widget.collapsed) ...[
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    widget.item.label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: fg,
                      fontSize: 14.5,
                      fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );

    tile = Semantics(button: true, selected: selected, label: widget.item.label, child: tile);
    if (widget.collapsed) {
      tile = Tooltip(message: widget.item.label, preferBelow: false, child: tile);
    }
    return tile;
  }
}

class _AdminFooter extends StatelessWidget {
  const _AdminFooter({required this.name, required this.role, required this.collapsed, required this.onLogout});

  final String name;
  final String role;
  final bool collapsed;
  final VoidCallback onLogout;

  @override
  Widget build(BuildContext context) {
    final logout = IconButton(
      tooltip: 'Cerrar sesión',
      onPressed: onLogout,
      icon: Icon(Icons.logout_rounded, color: Colors.white.withValues(alpha: 0.75), size: 20),
    );
    if (collapsed) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 14),
        child: Column(
          children: [
            Tooltip(message: '$name · $role', child: InitialsAvatar(name, size: 38, dark: true)),
            const SizedBox(height: 8),
            logout,
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 14, 10, 14),
      child: Row(
        children: [
          InitialsAvatar(name, size: 38, dark: true),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w700)),
                Text(role,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.55),
                        fontSize: 11.5,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.8)),
              ],
            ),
          ),
          logout,
        ],
      ),
    );
  }
}
